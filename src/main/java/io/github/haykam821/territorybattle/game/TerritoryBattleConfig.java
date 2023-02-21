package io.github.haykam821.territorybattle.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.haykam821.territorybattle.game.map.TerritoryBattleMapConfig;
import net.minecraft.SharedConstants;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.util.math.intprovider.ConstantIntProvider;
import net.minecraft.util.math.intprovider.IntProvider;
import xyz.nucleoid.plasmid.game.common.config.PlayerConfig;

public class TerritoryBattleConfig {
	public static final Codec<TerritoryBattleConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			TerritoryBattleMapConfig.CODEC.fieldOf("map").forGetter(TerritoryBattleConfig::getMapConfig),
			PlayerConfig.CODEC.fieldOf("players").forGetter(TerritoryBattleConfig::getPlayerConfig),
			IntProvider.NON_NEGATIVE_CODEC.optionalFieldOf("ticks_until_close", ConstantIntProvider.create(SharedConstants.TICKS_PER_SECOND * 5)).forGetter(TerritoryBattleConfig::getTicksUntilClose),
			RegistryCodecs.entryList(RegistryKeys.BLOCK).fieldOf("player_blocks").forGetter(TerritoryBattleConfig::getPlayerBlocks),
			Codec.INT.optionalFieldOf("time", 20 * 90).forGetter(TerritoryBattleConfig::getTime)
		).apply(instance, TerritoryBattleConfig::new);
	});

	private final TerritoryBattleMapConfig mapConfig;
	private final PlayerConfig playerConfig;
	private final IntProvider ticksUntilClose;
	private final RegistryEntryList<Block> playerBlocks;
	private final int time;

	public TerritoryBattleConfig(TerritoryBattleMapConfig mapConfig, PlayerConfig playerConfig, IntProvider ticksUntilClose, RegistryEntryList<Block> playerBlocks, int time) {
		this.mapConfig = mapConfig;
		this.playerConfig = playerConfig;
		this.ticksUntilClose = ticksUntilClose;
		this.playerBlocks = playerBlocks;
		this.time = time;
	}

	public TerritoryBattleMapConfig getMapConfig() {
		return this.mapConfig;
	}

	public PlayerConfig getPlayerConfig() {
		return this.playerConfig;
	}

	public IntProvider getTicksUntilClose() {
		return this.ticksUntilClose;
	}

	public RegistryEntryList<Block> getPlayerBlocks() {
		return this.playerBlocks;
	}
	
	public int getTime() {
		return this.time;
	}
}