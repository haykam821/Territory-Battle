package io.github.haykam821.territorybattle.mixin;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.util.math.Vec3i;
import xyz.nucleoid.plasmid.util.BlockTraversal;

@Mixin(value = BlockTraversal.Connectivity.class, remap = false)
public interface BlockTraversalConnectivityAccessor {
	@Invoker("create")
	public static BlockTraversal.Connectivity territorybattle$create(Consumer<Consumer<Vec3i>> generator) {
		throw new AssertionError();
	}
}
