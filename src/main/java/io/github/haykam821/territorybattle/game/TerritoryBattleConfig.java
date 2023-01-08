package io.github.haykam821.territorybattle.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.haykam821.territorybattle.game.map.TerritoryBattleMapConfig;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntryList;
import xyz.nucleoid.plasmid.game.common.config.PlayerConfig;

public class TerritoryBattleConfig {
	public static final Codec<TerritoryBattleConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			TerritoryBattleMapConfig.CODEC.fieldOf("map").forGetter(TerritoryBattleConfig::getMapConfig),
			PlayerConfig.CODEC.fieldOf("players").forGetter(TerritoryBattleConfig::getPlayerConfig),
			RegistryCodecs.entryList(RegistryKeys.BLOCK).fieldOf("player_blocks").forGetter(TerritoryBattleConfig::getPlayerBlocks),
			Codec.INT.optionalFieldOf("time", 20 * 90).forGetter(TerritoryBattleConfig::getTime)
		).apply(instance, TerritoryBattleConfig::new);
	});

	private final TerritoryBattleMapConfig mapConfig;
	private final PlayerConfig playerConfig;
	private final RegistryEntryList<Block> playerBlocks;
	private final int time;

	public TerritoryBattleConfig(TerritoryBattleMapConfig mapConfig, PlayerConfig playerConfig, RegistryEntryList<Block> playerBlocks, int time) {
		this.mapConfig = mapConfig;
		this.playerConfig = playerConfig;
		this.playerBlocks = playerBlocks;
		this.time = time;
	}

	public TerritoryBattleMapConfig getMapConfig() {
		return this.mapConfig;
	}

	public PlayerConfig getPlayerConfig() {
		return this.playerConfig;
	}

	public RegistryEntryList<Block> getPlayerBlocks() {
		return this.playerBlocks;
	}
	
	public int getTime() {
		return this.time;
	}
}