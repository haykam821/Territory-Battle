package io.github.haykam821.territorybattle.game.map;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.TextDisplayElement;
import net.minecraft.entity.decoration.DisplayEntity.BillboardMode;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class TerritoryBattleGuideText {
	private static final Text TITLE = Text.translatable("gameType.territorybattle.territory_battle").formatted(Formatting.BOLD);

	private static final Text TEXT = Text.empty()
			.append(TITLE)
			.append(ScreenTexts.LINE_BREAK)
			.append("Run over blocks to claim them as part of your territory.")
			.append(ScreenTexts.LINE_BREAK)
			.append("Once a block is claimed, it cannot be claimed by another player.")
			.append(ScreenTexts.LINE_BREAK)
			.append("Claim the most blocks before time runs out!")
			.formatted(Formatting.GOLD);

	private TerritoryBattleGuideText() {
		return;
	}

	public static ElementHolder createElementHolder() {
		TextDisplayElement element = new TextDisplayElement(TEXT);

		element.setBillboardMode(BillboardMode.CENTER);
		element.setLineWidth(350);
		element.setInvisible(true);

		ElementHolder holder = new ElementHolder();
		holder.addElement(element);

		return holder;
	}
}
