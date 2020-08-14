package io.github.haykam821.territorybattle.game.phase;

import com.google.common.collect.Lists;
import io.github.haykam821.territorybattle.game.PlayerTerritory;
import io.github.haykam821.territorybattle.game.TerritoryBattleConfig;
import io.github.haykam821.territorybattle.game.map.TerritoryBattleMap;
import io.github.haykam821.territorybattle.game.map.TerritoryBattleMapConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.Game;
import xyz.nucleoid.plasmid.game.GameWorld;
import xyz.nucleoid.plasmid.game.event.GameCloseListener;
import xyz.nucleoid.plasmid.game.event.GameOpenListener;
import xyz.nucleoid.plasmid.game.event.GameTickListener;
import xyz.nucleoid.plasmid.game.event.PlayerAddListener;
import xyz.nucleoid.plasmid.game.event.PlayerDeathListener;
import xyz.nucleoid.plasmid.game.rule.GameRule;
import xyz.nucleoid.plasmid.game.rule.RuleResult;
import xyz.nucleoid.plasmid.util.PlayerRef;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TerritoryBattleActivePhase {
	private static final Direction[] NEXT_TO_DIRECTIONS = new Direction[] {
		Direction.NORTH,
		Direction.EAST,
		Direction.SOUTH,
		Direction.WEST
	};

	private final ServerWorld world;
	private final GameWorld gameWorld;
	private final TerritoryBattleMap map;
	private final TerritoryBattleConfig config;
	private List<PlayerTerritory> territories;
	private int ticksLeft;
	private boolean opened;
	private ServerBossBar timerBar = new ServerBossBar(new LiteralText("Territory Battle"), BossBar.Color.BLUE, BossBar.Style.PROGRESS);

	public TerritoryBattleActivePhase(GameWorld gameWorld, TerritoryBattleMap map, TerritoryBattleConfig config, List<PlayerTerritory> territories) {
		this.world = gameWorld.getWorld();
		this.gameWorld = gameWorld;
		this.map = map;
		this.config = config;
		this.territories = territories;
		this.ticksLeft = this.config.getTime();
	}

	public static void setRules(Game game) {
		game.setRule(GameRule.CRAFTING, RuleResult.DENY);
		game.setRule(GameRule.FALL_DAMAGE, RuleResult.DENY);
		game.setRule(GameRule.HUNGER, RuleResult.DENY);
		game.setRule(GameRule.PORTALS, RuleResult.DENY);
		game.setRule(GameRule.PVP, RuleResult.DENY);
	}

	private static List<PlayerTerritory> getTerritories(Set<ServerPlayerEntity> players, List<Block> platformBlocks) {
		List<PlayerTerritory> territories = Lists.newArrayList();

		int index = 0;
		for (PlayerEntity player : players) {
			Block platformBlock = platformBlocks.get(index);

			territories.add(new PlayerTerritory(PlayerRef.of(player), platformBlock.getDefaultState()));

			index += 1;
			if (index > platformBlocks.size()) {
				index = 0;
			}
		}

		return territories;
	}

	public static void open(GameWorld gameWorld, TerritoryBattleMap map, TerritoryBattleConfig config) {
		List<Block> platformBlocks = config.getPlatformBlocks().values();
		List<PlayerTerritory> territories = TerritoryBattleActivePhase.getTerritories(gameWorld.getPlayers(), platformBlocks);

		TerritoryBattleActivePhase phase = new TerritoryBattleActivePhase(gameWorld, map, config, territories);

		gameWorld.openGame(game -> {
			TerritoryBattleActivePhase.setRules(game);

			// Listeners
			game.on(GameCloseListener.EVENT, phase::close);
			game.on(GameOpenListener.EVENT, phase::open);
			game.on(GameTickListener.EVENT, phase::tick);
			game.on(PlayerAddListener.EVENT, phase::addPlayer);
			game.on(PlayerDeathListener.EVENT, phase::onPlayerDeath);
		});
	}

	private void close() {
		this.timerBar.clearPlayers();
		this.timerBar.setVisible(false);
	}

	private double getDistance() {
		TerritoryBattleMapConfig mapConfig = this.config.getMapConfig();

		if (mapConfig.x < mapConfig.z) {
			return (this.config.getMapConfig().x - 3) / (double) 2;
		} else {
			return (this.config.getMapConfig().z - 3) / (double) 2;
		}
	}

	private void open() {
		this.opened = true;

		double distance = this.getDistance();
		for (int i = 0; i < this.territories.size(); i++) {
			PlayerTerritory territory = this.territories.get(i);
			ServerPlayerEntity player = territory.getPlayerRef().getEntity(this.world);
			if (player != null) {
				player.inventory.clear();
				territory.giveTerritoryStack(player);

				player.setGameMode(GameMode.ADVENTURE);

				double theta = ((double) i / this.territories.size()) * 2 * Math.PI;
				this.spawn(player, theta, distance);

				this.world.setBlockState(player.getBlockPos().down(), territory.getTerritoryState());
			}
		}
	}

	private boolean isNextToState(BlockPos pos, BlockState state) {
		for (Direction direction : NEXT_TO_DIRECTIONS) {
			if (this.world.getBlockState(pos.offset(direction)) == state) {
				return true;
			}
		}
		return false;
	}

	private void tick() {
		for (PlayerTerritory territory : this.territories) {
			territory.getPlayerRef().ifOnline(this.world, player -> {
				BlockPos landingPos = player.getLandingPos();

				BlockState state = this.world.getBlockState(landingPos);
				if (state != this.config.getMapConfig().getFloor()) return;

				BlockState territoryState = territory.getTerritoryState();
				if (this.isNextToState(landingPos, territoryState)) {
					this.world.setBlockState(landingPos, territoryState);
					this.world.playSound(null, landingPos, SoundEvents.BLOCK_SNOW_PLACE, SoundCategory.BLOCKS, 0.5f, 1);

					territory.setSize(territory.getSize() + 1);
				}
			});
		}

		this.ticksLeft -= 1;
 		this.timerBar.setPercent(this.ticksLeft / (float) this.config.getTime());
		if (this.ticksLeft == 0) {
			Text endingMessage = this.getEndingMessage();
			for (ServerPlayerEntity player : this.gameWorld.getPlayers()) {
				player.sendMessage(endingMessage, false);
			}

			this.gameWorld.close();
		}
	}

	private Text getEndingMessage() {
		if (this.territories.size() == 0) {
			return new LiteralText("Nobody won the game!").formatted(Formatting.RED);
		}

		List<PlayerTerritory> sortedTerritories = this.territories.stream().sorted().collect(Collectors.toList());
		PlayerTerritory winnerTerritory = sortedTerritories.get(sortedTerritories.size() - 1);
		return winnerTerritory.getWinMessage(this.world);
	}

	private void setSpectator(PlayerEntity player) {
		player.setGameMode(GameMode.SPECTATOR);
	}

	private void addPlayer(ServerPlayerEntity player) {
		if (this.opened) {
			this.setSpectator(player);
		}
		this.timerBar.addPlayer(player);
	}

	private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		// Respawn player
		TerritoryBattleWaitingPhase.spawn(this.gameWorld.getWorld(), this.map, player);
		return ActionResult.SUCCESS;
	}

	public void spawn(ServerPlayerEntity player, double theta, double distance) {
		Vec3d center = map.getPlatform().getCenter();

		double x = center.getX() + Math.sin(theta) * distance;
		double z = center.getZ() - Math.cos(theta) * distance;

		Vec3d pos = Vec3d.ofCenter(new BlockPos(x, 1, z));
		player.teleport(this.world, pos.getX(), 1, pos.getZ(), (float) Math.toDegrees(theta), 0);
	}
}
