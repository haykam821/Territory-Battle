package io.github.haykam821.territorybattle.game.map;

import io.github.haykam821.territorybattle.game.TerritoryBattleConfig;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import xyz.nucleoid.plasmid.map.template.MapTemplate;
import xyz.nucleoid.plasmid.util.BlockBounds;

public class TerritoryBattleMapBuilder {
	private final TerritoryBattleConfig config;

	public TerritoryBattleMapBuilder(TerritoryBattleConfig config) {
		this.config = config;
	}

	public TerritoryBattleMap create() {
		MapTemplate template = MapTemplate.createEmpty();
		TerritoryBattleMapConfig mapConfig = this.config.getMapConfig();

		BlockBounds bounds = new BlockBounds(BlockPos.ORIGIN, new BlockPos(mapConfig.x + 1, 2, mapConfig.z + 1));
		this.build(bounds, template, mapConfig);

		return new TerritoryBattleMap(template, bounds);
	}
	
	private BlockState getBlockState(BlockPos pos, BlockBounds bounds, TerritoryBattleMapConfig mapConfig) {
		int layer = pos.getY() - bounds.getMin().getY();
		boolean outline = pos.getX() == bounds.getMin().getX() || pos.getX() == bounds.getMax().getX() || pos.getZ() == bounds.getMin().getZ() || pos.getZ() == bounds.getMax().getZ();

		if (outline) {
			if (layer == 0) {
				return mapConfig.getFloorOutline();
			} else if (layer == 1) {
				return mapConfig.getWall();
			} else if (layer == 2) {
				return mapConfig.getWallTop();
			}
		} else if (layer == 0) {
			return mapConfig.getFloor();
		}

		return null;
	}

	public void build(BlockBounds bounds, MapTemplate template, TerritoryBattleMapConfig mapConfig) {
		for (BlockPos pos : bounds) {
			BlockState state = this.getBlockState(pos, bounds, mapConfig);
			if (state != null) {
				template.setBlockState(pos, state);
			}
		}
	}
}
