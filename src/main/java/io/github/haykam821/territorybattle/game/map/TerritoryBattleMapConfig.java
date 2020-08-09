package io.github.haykam821.territorybattle.game.map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

public class TerritoryBattleMapConfig {
	public static final Codec<TerritoryBattleMapConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			Codec.INT.fieldOf("x").forGetter(map -> map.x),
			Codec.INT.fieldOf("z").forGetter(map -> map.z),
			BlockState.CODEC.optionalFieldOf("platform", Blocks.WHITE_WOOL.getDefaultState()).forGetter(TerritoryBattleMapConfig::getPlatform)
		).apply(instance, TerritoryBattleMapConfig::new);
	});

	public final int x;
	public final int z;
	private final BlockState platform;

	public TerritoryBattleMapConfig(int x, int z, BlockState platform) {
		this.x = x;
		this.z = z;
		this.platform = platform;
	}

	public BlockState getPlatform() {
		return this.platform;
	}
}