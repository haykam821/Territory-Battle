package io.github.haykam821.territorybattle.game.map;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.plasmid.game.world.generator.TemplateChunkGenerator;

public class TerritoryBattleMap {
	private final MapTemplate template;

	private final BlockBounds platform;
	private final BlockBounds territoryBounds;

	private final Vec3d waitingSpawnPos;

	public TerritoryBattleMap(MapTemplate template, BlockBounds platform) {
		this.template = template;

		this.platform = platform;

		int territoryY = this.platform.min().getY();
		this.territoryBounds = BlockBounds.of(this.platform.min().getX() + 1, territoryY, this.platform.min().getZ() + 1, this.platform.max().getX() - 1, territoryY, this.platform.max().getZ() - 1);

		Vec3d center = this.platform.center();
		this.waitingSpawnPos = new Vec3d(center.getX(), 1, center.getZ());
	}

	public BlockBounds getPlatform() {
		return this.platform;
	}

	public BlockBounds getTerritoryBounds() {
		return this.territoryBounds;
	}

	public Vec3d getWaitingSpawnPos() {
		return this.waitingSpawnPos;
	}

	public ChunkGenerator createGenerator(MinecraftServer server) {
		return new TemplateChunkGenerator(server, this.template);
	}
}
