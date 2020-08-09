package io.github.haykam821.territorybattle.game.phase;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import io.github.haykam821.territorybattle.game.PlayerTerritory;
import io.github.haykam821.territorybattle.game.TerritoryBattleConfig;
import io.github.haykam821.territorybattle.game.map.TerritoryBattleMap;
import io.github.haykam821.territorybattle.game.map.TerritoryBattleMapConfig;
import net.gegy1000.plasmid.game.Game;
import net.gegy1000.plasmid.game.GameWorld;
import net.gegy1000.plasmid.game.event.GameCloseListener;
import net.gegy1000.plasmid.game.event.GameOpenListener;
import net.gegy1000.plasmid.game.event.GameTickListener;
import net.gegy1000.plasmid.game.event.PlayerAddListener;
import net.gegy1000.plasmid.game.event.PlayerDeathListener;
import net.gegy1000.plasmid.game.rule.GameRule;
import net.gegy1000.plasmid.game.rule.RuleResult;
import net.gegy1000.plasmid.util.PlayerRef;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

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
		game.setRule(GameRule.ALLOW_CRAFTING, RuleResult.DENY);
		game.setRule(GameRule.ALLOW_PORTALS, RuleResult.DENY);
		game.setRule(GameRule.ALLOW_PVP, RuleResult.DENY);
		game.setRule(GameRule.FALL_DAMAGE, RuleResult.DENY);
		game.setRule(GameRule.ENABLE_HUNGER, RuleResult.DENY);
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

		gameWorld.newGame(game -> {
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

		int angle = 0;
		double distance = this.getDistance();
 		for (PlayerTerritory territory : this.territories) {
			ServerPlayerEntity player = territory.getPlayerRef().getEntity(this.world);
			if (player != null) {
				player.inventory.clear();
				territory.giveTerritoryStack(player);

				player.setGameMode(GameMode.ADVENTURE);
				this.spawn(player, angle, distance);
				angle += 1 / (double) (this.territories.size() + 1);

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
				if (state != this.config.getMapConfig().getPlatform()) return;

				BlockState territoryState = territory.getTerritoryState();
				if (this.isNextToState(landingPos, territoryState)) {
					this.world.setBlockState(landingPos, territoryState);
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

			this.gameWorld.closeWorld();
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

	private boolean onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		// Respawn player
		TerritoryBattleWaitingPhase.spawn(this.gameWorld.getWorld(), this.map, player);
		return true;
	}

	public void spawn(ServerPlayerEntity player, int angle, double distance) {
		Vec3d center = map.getPlatform().getCenter();

		double x = center.getX() + Math.cos(angle) * distance;
		double y = center.getY() + 0.5;
		double z = center.getZ() + Math.sin(angle) * distance;

		Vec3d pos = Vec3d.ofCenter(new BlockPos(x, y, z));
		player.teleport(this.world, pos.getX(), y, pos.getZ(), angle + 90, 0);
	}
}