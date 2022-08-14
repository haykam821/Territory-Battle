package io.github.haykam821.territorybattle.game;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import xyz.nucleoid.plasmid.util.PlayerRef;

public class PlayerTerritory implements Comparable<PlayerTerritory> {
	private final PlayerRef playerRef;
	private final BlockState territoryState;
	private int size = 0;

	public PlayerTerritory(PlayerRef playerRef, BlockState territoryState) {
		this.playerRef = playerRef;
		this.territoryState = territoryState;
	}

	public PlayerRef getPlayerRef() {
		return this.playerRef;
	}

	public BlockState getTerritoryState() {
		return territoryState;
	}

	public int getSize() {
		return this.size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	private ItemStack getTerritoryStack() {
		return new ItemStack(this.territoryState.getBlock());
	}

	public void giveTerritoryStack(ServerPlayerEntity player) {
		player.getInventory().setStack(8, this.getTerritoryStack());

		// Update inventory
		player.currentScreenHandler.sendContentUpdates();
		player.playerScreenHandler.onContentChanged(player.getInventory());
	}

	public Text getWinMessage(ServerWorld world) {
		PlayerEntity winner = this.getPlayerRef().getEntity(world);
		if (winner == null) {
			return Text.literal("The winner is offline!").formatted(Formatting.GOLD);
		}

		return winner.getDisplayName().copy()
			.append(" has won the game with a territory of " + this.getSize() + " blocks!")
			.formatted(Formatting.GOLD);
	}

	private String getSidebarEntryName(ServerWorld world) {
		PlayerEntity player = this.getPlayerRef().getEntity(world);
		return player == null ? "<Unknown>" : player.getEntityName();
	}

	public Text getSidebarEntryText(ServerWorld world) {
		Text sizeText = Text.literal(this.size + "").formatted(Formatting.GOLD);
		return Text.literal(this.getSidebarEntryName(world) + ": ").append(sizeText).styled(style -> {
			return style.withBold(true).withColor(Formatting.DARK_GRAY);
		});
	}

	@Override
	public int compareTo(PlayerTerritory other) {
		return this.size - other.size;
	}

	@Override
	public String toString() {
		return "PlayerTerritory{playerRef=" + this.getPlayerRef() + ", territoryState=" + this.getTerritoryState() + ", size=" + this.getSize() + "}";
	}
}