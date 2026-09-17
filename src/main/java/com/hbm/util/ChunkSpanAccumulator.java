package com.hbm.util;

import net.minecraft.tileentity.TileEntity;

import java.util.ArrayList;

/**
 * rebuildChunk runs on background ChunkRenderWorker threads with no per-RenderChunk lock held
 * for its body, and the same RenderChunk instance can be compiled by two workers concurrently.
 * Keeping the per-pass extent/TESR accumulators as instance state raced (concurrent ArrayList
 * mutation -> AIOOBE, issue #1546). Each worker thread processes one rebuildChunk at a time, so
 * a thread-local accumulator is correctly task-scoped: reset at HEAD, accumulated during, and
 * published into the CompiledChunk before setVisibility, all on the same thread.
 */
public final class ChunkSpanAccumulator {


    public static final ThreadLocal<ChunkSpanAccumulator> LOCAL = ThreadLocal.withInitial(ChunkSpanAccumulator::new);

    public  int negX, posX, negY, posY, negZ, posZ;
    public final ArrayList<TileEntity> spanningTesrs = new ArrayList<>();

    public void reset() {
        negX = 0;
        posX = 0;
        negY = 0;
        posY = 0;
        negZ = 0;
        posZ = 0;
        spanningTesrs.clear();
    }
}
