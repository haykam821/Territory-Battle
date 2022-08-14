package io.github.haykam821.territorybattle.game;

import io.github.haykam821.territorybattle.game.phase.TerritoryBattleActivePhase;
import net.minecraft.text.Text;
import xyz.nucleoid.plasmid.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.game.common.widget.SidebarWidget;

public class TerritoryBattleSidebar {
	private final SidebarWidget widget;
	private final TerritoryBattleActivePhase phase;

	public TerritoryBattleSidebar(GlobalWidgets widgets, TerritoryBattleActivePhase phase, Text title) {
		Text name = title.copy().styled(style -> {
			return style.withBold(true);
		});
		this.widget = widgets.addSidebar(name);

		this.phase = phase;
	}

	public void update() {
		this.widget.set(content -> {
			this.phase.getTerritories().stream()
				.sorted()
				.map(territory -> {
					return territory.getSidebarEntryText(this.phase.getWorld());
				})
				.forEach(content::add);
		});
	}
}
