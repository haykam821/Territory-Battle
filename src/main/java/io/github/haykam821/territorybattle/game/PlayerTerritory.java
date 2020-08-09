package io.github.haykam821.territorybattle.game;

import net.gegy1000.plasmid.util.PlayerRef;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

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
		player.inventory.setStack(8, this.getTerritoryStack());

		// Update inventory
		player.currentScreenHandler.sendContentUpdates();
		player.playerScreenHandler.onContentChanged(player.inventory);
		player.updateCursorStack();
	}

	public Text getWinMessage(ServerWorld world) {
		PlayerEntity winner = this.getPlayerRef().getEntity(world);
		if (winner == null) {
			return new LiteralText("The winner is offline!").formatted(Formatting.GOLD);
		}

		return winner.getDisplayName().shallowCopy()
			.append(" has won the game with a territory of " + this.getSize() + " blocks!")
			.formatted(Formatting.GOLD);
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