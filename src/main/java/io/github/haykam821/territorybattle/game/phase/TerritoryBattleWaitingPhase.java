package io.github.haykam821.territorybattle.game.phase;

import io.github.haykam821.territorybattle.game.TerritoryBattleConfig;
import io.github.haykam821.territorybattle.game.map.TerritoryBattleMap;
import io.github.haykam821.territorybattle.game.map.TerritoryBattleMapBuilder;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameWaitingLobby;
import xyz.nucleoid.plasmid.game.GameWorld;
import xyz.nucleoid.plasmid.game.StartResult;
import xyz.nucleoid.plasmid.game.event.PlayerAddListener;
import xyz.nucleoid.plasmid.game.event.PlayerDeathListener;
import xyz.nucleoid.plasmid.game.event.RequestStartListener;
import xyz.nucleoid.plasmid.world.bubble.BubbleWorldConfig;

import java.util.concurrent.CompletableFuture;

public class TerritoryBattleWaitingPhase {
	private final GameWorld gameWorld;
	private final TerritoryBattleMap map;
	private final TerritoryBattleConfig config;

	public TerritoryBattleWaitingPhase(GameWorld gameWorld, TerritoryBattleMap map, TerritoryBattleConfig config) {
		this.gameWorld = gameWorld;
		this.map = map;
		this.config = config;
	}

	public static CompletableFuture<GameWorld> open(GameOpenContext<TerritoryBattleConfig> context) {
		TerritoryBattleMapBuilder mapBuilder = new TerritoryBattleMapBuilder(context.getConfig());

		return mapBuilder.create().thenCompose(map -> {
			BubbleWorldConfig worldConfig = new BubbleWorldConfig()
				.setGenerator(map.createGenerator(context.getServer()))
				.setDefaultGameMode(GameMode.SPECTATOR);
			return context.openWorld(worldConfig).thenApply(gameWorld -> {
				TerritoryBattleWaitingPhase phase = new TerritoryBattleWaitingPhase(gameWorld, map, context.getConfig());

				return GameWaitingLobby.open(gameWorld, context.getConfig().getPlayerConfig(), game -> {
					TerritoryBattleActivePhase.setRules(game);

					// Listeners
					game.on(PlayerAddListener.EVENT, phase::addPlayer);
					game.on(PlayerDeathListener.EVENT, phase::onPlayerDeath);
					game.on(RequestStartListener.EVENT, phase::requestStart);
				});
			});
		});
	}

	private StartResult requestStart() {
		TerritoryBattleActivePhase.open(this.gameWorld, this.map, this.config);
		return StartResult.OK;
	}

	private void addPlayer(ServerPlayerEntity player) {
		TerritoryBattleWaitingPhase.spawn(this.gameWorld.getWorld(), this.map, player);
	}

	private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		// Respawn player
		TerritoryBattleWaitingPhase.spawn(this.gameWorld.getWorld(), this.map, player);
		return ActionResult.SUCCESS;
	}

	public static void spawn(ServerWorld world, TerritoryBattleMap map, ServerPlayerEntity player) {
		Vec3d center = map.getPlatform().getCenter();
		player.teleport(world, center.getX(), 1, center.getZ(), 0, 0);
	}
}
