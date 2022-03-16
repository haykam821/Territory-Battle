package io.github.haykam821.territorybattle;

import io.github.haykam821.territorybattle.game.TerritoryBattleConfig;
import io.github.haykam821.territorybattle.game.phase.TerritoryBattleWaitingPhase;
import net.fabricmc.api.ModInitializer;
import net.minecraft.block.Block;
import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import xyz.nucleoid.plasmid.game.GameType;

public class Main implements ModInitializer {
	public static final String MOD_ID = "territorybattle";

	private static final Identifier PLAYER_BLOCKS_ID = new Identifier(MOD_ID, "player_blocks");
	public static final TagKey<Block> PLAYER_BLOCKS = TagKey.of(Registry.BLOCK_KEY, PLAYER_BLOCKS_ID);

	private static final Identifier TERRITORY_BATTLE_ID = new Identifier(MOD_ID, "territory_battle");
	public static final GameType<TerritoryBattleConfig> TERRITORY_BATTLE_TYPE = GameType.register(TERRITORY_BATTLE_ID, TerritoryBattleConfig.CODEC, TerritoryBattleWaitingPhase::open);

	@Override
	public void onInitialize() {
		return;
	}
}