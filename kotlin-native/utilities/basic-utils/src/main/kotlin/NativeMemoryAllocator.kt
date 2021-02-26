/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.konan.util

import sun.misc.Unsafe

private val allocatorHolder = ThreadLocal<NativeMemoryAllocator>()
val nativeMemoryAllocator: NativeMemoryAllocator
    get() = allocatorHolder.get() ?: NativeMemoryAllocator().also { allocatorHolder.set(it) }

fun disposeNativeMemoryAllocator() {
    allocatorHolder.get()?.freeAll()
    allocatorHolder.remove()
}

// 256 buckets for sizes <= 2048 padded to 8
// 256 buckets for sizes <= 64KB padded to 256
// 256 buckets for sizes <= 1MB padded to 4096
private const val ChunkBucketSize = 256
// Alignments are such that overhead is approx 10%.
private const val SmallChunksSizeAlignment = 8
private const val MediumChunksSizeAlignment = 256
private const val BigChunksSizeAlignment = 4096
private const val MaxSmallSize = ChunkBucketSize * SmallChunksSizeAlignment
private const val MaxMediumSize = ChunkBucketSize * MediumChunksSizeAlignment
private const val MaxBigSize = ChunkBucketSize * BigChunksSizeAlignment
private const val ChunkHeaderSize = 2 * Int.SIZE_BYTES // chunk size + alignment hop size.

private const val RawChunkSize: Long = 4 * 1024 * 1024

class NativeMemoryAllocator {
    private fun alignUp(x: Long, align: Int) = (x + align - 1) and (align - 1).toLong().inv()
    private fun alignUp(x: Int, align: Int) = (x + align - 1) and (align - 1).inv()

    private val smallChunks = LongArray(ChunkBucketSize)
    private val mediumChunks = LongArray(ChunkBucketSize)
    private val bigChunks = LongArray(ChunkBucketSize)

    // Chunk layout: [chunk size,...padding...,diff to start,aligned data start,.....data.....]
    fun alloc(size: Long, align: Int): Long {
        val totalChunkSize = ChunkHeaderSize + size + align
        val ptr = ChunkHeaderSize + when {
            totalChunkSize <= MaxSmallSize -> allocFromFreeList(totalChunkSize.toInt(), SmallChunksSizeAlignment, smallChunks)
            totalChunkSize <= MaxMediumSize -> allocFromFreeList(totalChunkSize.toInt(), MediumChunksSizeAlignment, mediumChunks)
            totalChunkSize <= MaxBigSize -> allocFromFreeList(totalChunkSize.toInt(), BigChunksSizeAlignment, bigChunks)
            else -> unsafe.allocateMemory(totalChunkSize).also {
                // The actual size is not used. Just put value bigger than the biggest threshold.
                unsafe.putInt(it, Int.MAX_VALUE)
            }
        }
        val alignedPtr = alignUp(ptr, align)
        unsafe.putInt(alignedPtr - Int.SIZE_BYTES, (alignedPtr - ptr).toInt())
        return alignedPtr
    }

    private fun allocFromFreeList(size: Int, align: Int, freeList: LongArray): Long {
        val paddedSize = alignUp(size, align)
        val index = paddedSize / align - 1
        val chunk = freeList[index]
        val ptr = if (chunk == 0L)
            allocRaw(paddedSize)
        else {
            val nextChunk = unsafe.getLong(chunk)
            freeList[index] = nextChunk
            chunk
        }
        unsafe.putInt(ptr, paddedSize)
        return ptr
    }

    private fun freeToFreeList(paddedSize: Int, align: Int, freeList: LongArray, chunk: Long) {
        require(paddedSize > 0 && paddedSize % align == 0)
        val index = paddedSize / align - 1
        unsafe.putLong(chunk, freeList[index])
        freeList[index] = chunk
    }

    private val rawChunks = mutableListOf<Long>()
    private var rawOffset = 0

    private fun allocRaw(size: Int): Long {
        if (rawChunks.isEmpty() || rawOffset + size > RawChunkSize) {
            val newRawChunk = unsafe.allocateMemory(RawChunkSize)
            rawChunks.add(newRawChunk)
            rawOffset = size
            return newRawChunk
        }
        return (rawChunks.last() + rawOffset).also { rawOffset += size }
    }

    fun free(mem: Long) {
        val chunkStart = mem - ChunkHeaderSize - unsafe.getInt(mem - Int.SIZE_BYTES)
        val chunkSize = unsafe.getInt(chunkStart)
        when {
            chunkSize <= MaxSmallSize -> freeToFreeList(chunkSize, SmallChunksSizeAlignment, smallChunks, chunkStart)
            chunkSize <= MaxMediumSize -> freeToFreeList(chunkSize, MediumChunksSizeAlignment, mediumChunks, chunkStart)
            chunkSize <= MaxBigSize -> freeToFreeList(chunkSize, BigChunksSizeAlignment, bigChunks, chunkStart)
            else -> unsafe.freeMemory(chunkStart)
        }
    }

    fun freeAll() {
        for (i in 0 until ChunkBucketSize) {
            smallChunks[i] = 0L
            mediumChunks[i] = 0L
            bigChunks[i] = 0L
        }
        for (chunk in rawChunks)
            unsafe.freeMemory(chunk)
        rawChunks.clear()
    }

    private val unsafe = with(Unsafe::class.java.getDeclaredField("theUnsafe")) {
        isAccessible = true
        return@with this.get(null) as Unsafe
    }
}