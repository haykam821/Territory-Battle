package io.github.haykam821.territorybattle.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.haykam821.territorybattle.Main;
import io.github.haykam821.territorybattle.game.map.TerritoryBattleMapConfig;
import net.gegy1000.plasmid.game.config.GameConfig;
import net.gegy1000.plasmid.game.config.PlayerConfig;
import net.minecraft.block.Block;
import net.minecraft.tag.BlockTags;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;

public class TerritoryBattleConfig implements GameConfig {
	public static final Codec<TerritoryBattleConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			TerritoryBattleMapConfig.CODEC.fieldOf("map").forGetter(TerritoryBattleConfig::getMapConfig),
			PlayerConfig.CODEC.fieldOf("players").forGetter(TerritoryBattleConfig::getPlayerConfig),
			Identifier.CODEC.fieldOf("playerBlocks").forGetter(TerritoryBattleConfig::getPlayerBlocksId),
			Codec.INT.optionalFieldOf("time", 20 * 90).forGetter(TerritoryBattleConfig::getTime)
		).apply(instance, TerritoryBattleConfig::new);
	});

	private final TerritoryBattleMapConfig mapConfig;
	private final PlayerConfig playerConfig;
	private final Identifier playerBlocksId;
	private final int time;

	public TerritoryBattleConfig(TerritoryBattleMapConfig mapConfig, PlayerConfig playerConfig, Identifier playerBlocksId, int time) {
		this.mapConfig = mapConfig;
		this.playerConfig = playerConfig;
		this.playerBlocksId = playerBlocksId;
		this.time = time;
	}

	public TerritoryBattleMapConfig getMapConfig() {
		return this.mapConfig;
	}

	public PlayerConfig getPlayerConfig() {
		return this.playerConfig;
	}

	public Identifier getPlayerBlocksId() {
		return playerBlocksId;
	}

	public Tag<Block> getPlatformBlocks() {
		Tag<Block> tag = BlockTags.getContainer().get(this.playerBlocksId);
		return tag == null ? Main.PLAYER_BLOCKS : tag;
	}
	
	public int getTime() {
		return this.time;
	}
}