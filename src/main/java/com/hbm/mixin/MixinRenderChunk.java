package com.hbm.mixin;

import com.hbm.render.chunk.ChunkSpanningTesrHelper;
import com.hbm.render.chunk.IExtraExtentsHolder;
import com.hbm.render.chunk.IShadowRenderFrameStamp;
import com.hbm.util.ChunkSpanAccumulator;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.chunk.SetVisibility;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderChunk.class)
public abstract class MixinRenderChunk implements IShadowRenderFrameStamp {

    @Unique
    private int hbm$shadowFrameStamp = Integer.MIN_VALUE;

    @Shadow
    public AxisAlignedBB boundingBox;
    @Shadow
    private int frameIndex;
    @Final
    @Shadow
    private BlockPos.MutableBlockPos position;
    @Shadow
    public CompiledChunk compiledChunk;

    @Override
    public int hbm$getFrameStamp() {
        return frameIndex;
    }

    @Override
    public void hbm$setFrameStamp(int frame) {
        frameIndex = frame;
    }

    @Override
    public int hbm$getShadowFrameStamp() {
        return hbm$shadowFrameStamp;
    }

    @Override
    public void hbm$setShadowFrameStamp(int frame) {
        hbm$shadowFrameStamp = frame;
    }

    @Inject(method = "rebuildChunk", at = @At("HEAD"), require = 1)
    private void hbm$resetOversizedExtents(float x, float y, float z, net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator generator, CallbackInfo ci) {
        ChunkSpanAccumulator.LOCAL.get().reset();
    }
    @WrapOperation(method = "rebuildChunk", require = 1, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/tileentity/TileEntitySpecialRenderer;isGlobalRenderer(Lnet/minecraft/tileentity/TileEntity;)Z"))
    private boolean hbm$trackOversizedTesr(TileEntitySpecialRenderer<TileEntity> renderer, TileEntity te, Operation<Boolean> original) {
        if (original.call(renderer, te))
            return true;
        AxisAlignedBB bb = te.getRenderBoundingBox();
        if (bb == TileEntity.INFINITE_EXTENT_AABB)
            return true;
        int sx = position.getX();
        int sy = position.getY();
        int sz = position.getZ();
        int negX = (int) Math.ceil(Math.max(0.0D, sx - bb.minX));
        int posX = (int) Math.ceil(Math.max(0.0D, bb.maxX - (sx + 16.0D)));
        int negY = (int) Math.ceil(Math.max(0.0D, sy - bb.minY));
        int posY = (int) Math.ceil(Math.max(0.0D, bb.maxY - (sy + 16.0D)));
        int negZ = (int) Math.ceil(Math.max(0.0D, sz - bb.minZ));
        int posZ = (int) Math.ceil(Math.max(0.0D, bb.maxZ - (sz + 16.0D)));
        if ((negX | posX | negY | posY | negZ | posZ) != 0) {
            ChunkSpanAccumulator acc = ChunkSpanAccumulator.LOCAL.get();
            acc.negX = Math.max(acc.negX, negX);
            acc.posX = Math.max(acc.posX, posX);
            acc.negY = Math.max(acc.negY, negY);
            acc.posY = Math.max(acc.posY, posY);
            acc.negZ = Math.max(acc.negZ, negZ);
            acc.posZ = Math.max(acc.posZ, posZ);
            acc.spanningTesrs.add(te);
        }
        return false;
    }

    @WrapOperation(method = "rebuildChunk", require = 1, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/CompiledChunk;setVisibility(Lnet/minecraft/client/renderer/chunk/SetVisibility;)V"))
    private void hbm$publishOversizedExtents(CompiledChunk compiledChunk, SetVisibility visibility, Operation<Void> original) {
        IExtraExtentsHolder holder = (IExtraExtentsHolder) compiledChunk;
        ChunkSpanAccumulator acc = ChunkSpanAccumulator.LOCAL.get();
        holder.hbm$setOversizedModelExtents(acc.negX, acc.posX, acc.negY, acc.posY, acc.negZ, acc.posZ);
        if (!acc.spanningTesrs.isEmpty()) {
            holder.hbm$setChunkSpanningTesrs(acc.spanningTesrs.toArray(new TileEntity[0]));
        }
        //noinspection MixinExtrasOperationParameters
        original.call(compiledChunk, visibility);
    }

    @Inject(method = "setCompiledChunk", at = @At("HEAD"), require = 1)
    private void hbm$updateChunkSpanningTesrSet(CompiledChunk compiledChunkIn, CallbackInfo ci) {
        TileEntity[] old = ((IExtraExtentsHolder) compiledChunk).hbm$getChunkSpanningTesrs();
        TileEntity[] nu = ((IExtraExtentsHolder) compiledChunkIn).hbm$getChunkSpanningTesrs();
        if (old.length != 0 || nu.length != 0) {
            ChunkSpanningTesrHelper.updateChunkSpanningTesrs(old, nu);
        }
    }

    /** stopCompileTask assigns compiledChunk = DUMMY directly, bypassing setCompiledChunk. */
    @Inject(method = "stopCompileTask", at = @At("HEAD"), require = 1)
    private void hbm$drainOnStop(CallbackInfo ci) {
        TileEntity[] old = ((IExtraExtentsHolder) compiledChunk).hbm$getChunkSpanningTesrs();
        if (old.length != 0) {
            ChunkSpanningTesrHelper.updateChunkSpanningTesrs(old, IExtraExtentsHolder.EMPTY_TE_ARR);
        }
    }

    @Inject(method = "setCompiledChunk", at = @At("RETURN"), require = 1)
    private void hbm$applyExpansion(CompiledChunk compiledChunkIn, CallbackInfo ci) {
        double minX = position.getX();
        double minY = position.getY();
        double minZ = position.getZ();
        IExtraExtentsHolder holder = (IExtraExtentsHolder) compiledChunkIn;

        boundingBox = new AxisAlignedBB(
                minX - holder.hbm$getNegX(),
                minY - holder.hbm$getNegY(),
                minZ - holder.hbm$getNegZ(),
                minX + 16.0D + holder.hbm$getPosX(),
                minY + 16.0D + holder.hbm$getPosY(),
                minZ + 16.0D + holder.hbm$getPosZ());
    }
}
