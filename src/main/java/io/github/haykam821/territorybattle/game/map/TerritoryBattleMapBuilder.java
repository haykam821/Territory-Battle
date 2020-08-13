package io.github.haykam821.territorybattle.game.map;

import java.util.concurrent.CompletableFuture;

import io.github.haykam821.territorybattle.game.TerritoryBattleConfig;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import xyz.nucleoid.plasmid.game.map.template.MapTemplate;
import xyz.nucleoid.plasmid.util.BlockBounds;

public class TerritoryBattleMapBuilder {
	private final TerritoryBattleConfig config;

	public TerritoryBattleMapBuilder(TerritoryBattleConfig config) {
		this.config = config;
	}

	public CompletableFuture<TerritoryBattleMap> create() {
		return CompletableFuture.supplyAsync(() -> {
			MapTemplate template = MapTemplate.createEmpty();
			TerritoryBattleMapConfig mapConfig = this.config.getMapConfig();

			BlockBounds bounds = new BlockBounds(BlockPos.ORIGIN, new BlockPos(mapConfig.x - 1, 0, mapConfig.z - 1));
			this.build(bounds, template);

			return new TerritoryBattleMap(template, bounds);
		}, Util.getMainWorkerExecutor());
	}

	public void build(BlockBounds bounds, MapTemplate template) {
		for (BlockPos pos : bounds.iterate()) {
			template.setBlockState(pos, config.getMapConfig().getPlatform());
		}
	}
}