package com.hbm.mixin.vanilla.betterfoliage;

import com.hbm.main.client.StaticTesrBakedModels;
import com.hbm.util.ChunkSpanAccumulator;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Better-Foliage-aware counterpart of {@link MixinRenderChunkBlockTrackVanilla}.
 */
@Mixin(RenderChunk.class)
public abstract class MixinRenderChunkBlockTrackBetterFoliage {

    @Final
    @Shadow
    private BlockPos.MutableBlockPos position;


    @Dynamic("Better Foliage coremod replaces RenderChunk#rebuildChunk's renderBlock call with Hooks.renderWorldBlock")
    @WrapOperation(method = "rebuildChunk", at = @At(value = "INVOKE", target = "Lmods/betterfoliage/client/Hooks;renderWorldBlock", remap = false), require = 1)
    private boolean hbm$trackOversizedBlockBetterFoliage(BlockRendererDispatcher dispatcher, IBlockState state, BlockPos pos,
                                                         IBlockAccess world, BufferBuilder buffer, BlockRenderLayer layer,
                                                         Operation<Boolean> original) {
        boolean result = original.call(dispatcher, state, pos, world, buffer, layer);
        if (result) {
            int[] extents = StaticTesrBakedModels.getManagedRenderExtents(state);
            if (extents != null) {
                int localX = pos.getX() - position.getX();
                int localY = pos.getY() - position.getY();
                int localZ = pos.getZ() - position.getZ();

                ChunkSpanAccumulator acc = ChunkSpanAccumulator.LOCAL.get();
                acc.negX = Math.max(acc.negX, extents[4] - localX);
                acc.posX = Math.max(acc.posX, localX + extents[5] - 15);
                acc.negY = Math.max(acc.negY, extents[1] - localY);
                acc.posY = Math.max(acc.posY, localY + extents[0] - 15);
                acc.negZ = Math.max(acc.negZ, extents[2] - localZ);
                acc.posZ = Math.max(acc.posZ, localZ + extents[3] - 15);
            }
        }
        return result;
    }
}
