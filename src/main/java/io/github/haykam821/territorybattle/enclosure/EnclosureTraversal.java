package io.github.haykam821.territorybattle.enclosure;

import io.github.haykam821.territorybattle.mixin.BlockTraversalConnectivityAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.plasmid.util.BlockTraversal;
import xyz.nucleoid.plasmid.util.BlockTraversal.Connectivity;
import xyz.nucleoid.plasmid.util.BlockTraversal.Result;

/**
 * Generic utilities for determining whether a floor region is enclosed by a certain block state.
 */
public final class EnclosureTraversal {
	private static final Connectivity FOUR = BlockTraversalConnectivityAccessor.territorybattle$create(consumer -> {
		for (Direction direction : Direction.Type.HORIZONTAL) {
			consumer.accept(direction.getVector());
		}
	});

	private static final BlockTraversal TRAVERSAL = BlockTraversal.create().connectivity(FOUR);

	private EnclosureTraversal() {
		return;
	}

	public static EnclosureResult findEnclosure(ServerWorld world, BlockPos origin, BlockBounds bounds, BlockState outlineState, BlockState floorState) {
		EnclosureResult result = new EnclosureResult();

		TRAVERSAL.accept(origin, (pos, fromPos, depth) -> {
			if (result.isUnenclosed() || !bounds.contains(pos)) {
				return Result.TERMINATE;
			}

			BlockState state = world.getBlockState(pos);

			if (state == outlineState) {
				return Result.TERMINATE;
			} else if (state != floorState) {
				result.markUnenclosed();
				return Result.TERMINATE;
			}

			result.addPosition(pos);
			return Result.CONTINUE;
		});

		return result;
	}
}
