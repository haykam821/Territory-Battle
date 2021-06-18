package io.github.haykam821.territorybattle.game.phase;

import com.google.common.collect.Lists;
import io.github.haykam821.territorybattle.game.PlayerTerritory;
import io.github.haykam821.territorybattle.game.TerritoryBattleConfig;
import io.github.haykam821.territorybattle.game.TerritoryBattleSidebar;
import io.github.haykam821.territorybattle.game.map.TerritoryBattleMap;
import io.github.haykam821.territorybattle.game.map.TerritoryBattleMapConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.boss.BossBar;
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
import xyz.nucleoid.plasmid.game.GameCloseReason;
import xyz.nucleoid.plasmid.game.GameLogic;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.event.GameOpenListener;
import xyz.nucleoid.plasmid.game.event.GameTickListener;
import xyz.nucleoid.plasmid.game.event.PlayerAddListener;
import xyz.nucleoid.plasmid.game.event.PlayerDeathListener;
import xyz.nucleoid.plasmid.game.rule.GameRule;
import xyz.nucleoid.plasmid.game.rule.RuleResult;
import xyz.nucleoid.plasmid.util.PlayerRef;
import xyz.nucleoid.plasmid.widget.BossBarWidget;
import xyz.nucleoid.plasmid.widget.GlobalWidgets;

import java.util.List;
import java.util.stream.Collectors;

public class TerritoryBattleActivePhase {
	private static final Direction[] NEXT_TO_DIRECTIONS = new Direction[] {
		Direction.NORTH,
		Direction.EAST,
		Direction.SOUTH,
		Direction.WEST
	};

	private final ServerWorld world;
	private final GameSpace gameSpace;
	private final TerritoryBattleMap map;
	private final TerritoryBattleConfig config;
	private List<PlayerTerritory> territories;
	private int ticksLeft;
	private boolean opened;
	private BossBarWidget timerBar;
	private final TerritoryBattleSidebar sidebar;
	private int availableTerritory;

	public TerritoryBattleActivePhase(GameSpace gameSpace, TerritoryBattleMap map, TerritoryBattleConfig config, List<PlayerTerritory> territories, GlobalWidgets widgets) {
		this.world = gameSpace.getWorld();
		this.gameSpace = gameSpace;
		this.map = map;
		this.config = config;
		this.territories = territories;
		this.ticksLeft = this.config.getTime();
		this.availableTerritory = this.config.getMapConfig().x * this.config.getMapConfig().z;

		LiteralText timerTitle = new LiteralText("Territory Battle");
		this.timerBar = widgets.addBossBar(timerTitle, BossBar.Color.BLUE, BossBar.Style.PROGRESS);
		this.sidebar = new TerritoryBattleSidebar(widgets, this, timerTitle);
	}

	public static void setRules(GameLogic game) {
		game.setRule(GameRule.CRAFTING, RuleResult.DENY);
		game.setRule(GameRule.FALL_DAMAGE, RuleResult.DENY);
		game.setRule(GameRule.HUNGER, RuleResult.DENY);
		game.setRule(GameRule.PORTALS, RuleResult.DENY);
		game.setRule(GameRule.PVP, RuleResult.DENY);
		game.setRule(GameRule.THROW_ITEMS, RuleResult.DENY);
	}

	private static List<PlayerTerritory> getTerritories(Iterable<ServerPlayerEntity> players, List<Block> platformBlocks) {
		List<PlayerTerritory> territories = Lists.newArrayList();

		int index = 0;
		for (PlayerEntity player : players) {
			Block platformBlock = platformBlocks.get(index);

			territories.add(new PlayerTerritory(PlayerRef.of(player), platformBlock.getDefaultState()));

			index += 1;
			if (index >= platformBlocks.size()) {
				index = 0;
			}
		}

		return territories;
	}

	public static void open(GameSpace gameSpace, TerritoryBattleMap map, TerritoryBattleConfig config) {
		gameSpace.openGame(game -> {
			GlobalWidgets widgets = new GlobalWidgets(game);

			List<Block> platformBlocks = config.getPlatformBlocks().values();
			List<PlayerTerritory> territories = TerritoryBattleActivePhase.getTerritories(gameSpace.getPlayers(), platformBlocks);

			TerritoryBattleActivePhase phase = new TerritoryBattleActivePhase(gameSpace, map, config, territories, widgets);

			TerritoryBattleActivePhase.setRules(game);

			// Listeners
			game.on(GameOpenListener.EVENT, phase::open);
			game.on(GameTickListener.EVENT, phase::tick);
			game.on(PlayerAddListener.EVENT, phase::addPlayer);
			game.on(PlayerDeathListener.EVENT, phase::onPlayerDeath);
		});
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
				this.availableTerritory -= 1;
			}
		}

		this.sidebar.update();
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
		boolean territoryUpdated = false;
		for (PlayerTerritory territory : this.territories) {
			ServerPlayerEntity player = territory.getPlayerRef().getEntity(this.world);
			if (player != null) {
				BlockPos landingPos = player.getLandingPos();

				BlockState state = this.world.getBlockState(landingPos);
				if (state != this.config.getMapConfig().getFloor()) return;

				BlockState territoryState = territory.getTerritoryState();
				if (this.isNextToState(landingPos, territoryState)) {
					this.world.setBlockState(landingPos, territoryState);
					this.world.playSound(null, landingPos, SoundEvents.BLOCK_SNOW_PLACE, SoundCategory.BLOCKS, 0.5f, 1);

					territory.setSize(territory.getSize() + 1);
					this.availableTerritory -= 1;

					territoryUpdated = true;
				}
			}
		}

		if (territoryUpdated) {
			this.sidebar.update();
		}

		this.ticksLeft -= 1;
 		this.timerBar.setProgress(this.ticksLeft / (float) this.config.getTime());
		if (this.ticksLeft == 0 || this.availableTerritory <= 0) {
			this.gameSpace.getPlayers().sendMessage(this.getEndingMessage());

			this.gameSpace.close(GameCloseReason.FINISHED);
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
	}

	private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		// Respawn player
		TerritoryBattleWaitingPhase.spawn(this.gameSpace.getWorld(), this.map, player);
		return ActionResult.SUCCESS;
	}

	public void spawn(ServerPlayerEntity player, double theta, double distance) {
		Vec3d center = map.getPlatform().getCenter();

		double x = center.getX() + Math.sin(theta) * distance;
		double z = center.getZ() - Math.cos(theta) * distance;

		Vec3d pos = Vec3d.ofCenter(new BlockPos(x, 1, z));
		player.teleport(this.world, pos.getX(), 1, pos.getZ(), (float) Math.toDegrees(theta), 0);
	}

	public ServerWorld getWorld() {
		return this.world;
	}

	public List<PlayerTerritory> getTerritories() {
		return this.territories;
	}
}
