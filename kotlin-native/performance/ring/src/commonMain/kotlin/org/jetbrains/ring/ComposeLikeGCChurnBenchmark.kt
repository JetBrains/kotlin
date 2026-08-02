/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(kotlin.native.concurrent.ObsoleteWorkersApi::class, kotlin.native.runtime.NativeRuntimeApi::class)

package org.jetbrains.ring

import kotlin.native.concurrent.Worker
import kotlin.native.runtime.GC
import kotlinx.benchmark.*
import org.jetbrains.benchmarksLauncher.SkipWhenBaseOnly

private const val COMPOSE_LIKE_TREE_DEPTH = 7
private const val COMPOSE_LIKE_CHILDREN_PER_NODE = 4
private const val COMPOSE_LIKE_MODIFIERS_PER_NODE = 4
private const val COMPOSE_LIKE_STATE_UPDATES_PER_FRAME = 96
private const val COMPOSE_LIKE_VISIBLE_NODES_PER_FRAME = 1024
private const val COMPOSE_LIKE_FRAMES_PER_OPERATION = 16
private const val COMPOSE_LIKE_GC_SCHEDULE_PERIOD = COMPOSE_LIKE_FRAMES_PER_OPERATION
private const val COMPOSE_LIKE_IDLE_AFTER_GC_SCHEDULE_MICROS = 0L
private const val COMPOSE_LIKE_GC_WAIT_SPINS = 100_000
private const val COMPOSE_LIKE_FULL_GROWTH_TRIGGER_PERCENT = 1_000L

private class ComposeLikeState(
    var version: Int,
    var value: Int,
)

private class ComposeLikeModifier(
    val kind: Int,
    val weight: Int,
    val next: ComposeLikeModifier?,
)

private class ComposeLikeNode(
    val id: Int,
    val children: Array<ComposeLikeNode?>,
    val state: ComposeLikeState,
    val modifier: ComposeLikeModifier?,
) {
    var dirtyVersion: Int = 0
}

private class ComposeLikeGroup(
    val node: ComposeLikeNode,
    val stateVersion: Int,
    val stateValue: Int,
    val changeMask: Int,
)

private class ComposeLikeMeasureInput(
    val maxWidth: Int,
    val maxHeight: Int,
    val density: Int,
)

private class ComposeLikeMeasureResult(
    val node: ComposeLikeNode,
    val width: Int,
    val height: Int,
    val baseline: Int,
)

private class ComposeLikeDrawOp(
    val measureResult: ComposeLikeMeasureResult,
    val color: Int,
    val previous: ComposeLikeDrawOp?,
)

private class ComposeLikeScene {
    val root: ComposeLikeNode
    val nodes: Array<ComposeLikeNode>
    private var frame = 0

    init {
        val allNodes = ArrayList<ComposeLikeNode>()
        root = newNode(depth = 0, allNodes)
        nodes = allNodes.toTypedArray()
    }

    fun composeFrame(): Int {
        frame += 1

        var checksum = root.id + frame
        val recomposedGroups = arrayOfNulls<ComposeLikeGroup>(COMPOSE_LIKE_STATE_UPDATES_PER_FRAME)
        repeat(COMPOSE_LIKE_STATE_UPDATES_PER_FRAME) { update ->
            val node = nodes[indexFor(frame, update, stride = 8191)]
            val state = node.state
            state.version = frame
            state.value += node.id + update
            node.dirtyVersion = frame

            val group = ComposeLikeGroup(node, state.version, state.value, changeMaskFor(node))
            recomposedGroups[update] = group
            checksum += group.changeMask + group.stateValue
        }

        var previousDrawOp: ComposeLikeDrawOp? = null
        val drawOps = arrayOfNulls<ComposeLikeDrawOp>(COMPOSE_LIKE_VISIBLE_NODES_PER_FRAME)
        repeat(COMPOSE_LIKE_VISIBLE_NODES_PER_FRAME) { slot ->
            val node = nodes[indexFor(frame, slot, stride = 257)]
            val input = ComposeLikeMeasureInput(
                maxWidth = 320 + (slot and 31),
                maxHeight = 48 + (node.state.value and 31),
                density = 2 + (slot and 1),
            )
            val measured = measure(node, input)
            val drawOp = ComposeLikeDrawOp(
                measured,
                color = (measured.width * 31 + measured.height * 17 + node.id) and 0x00ff_ffff,
                previous = previousDrawOp,
            )
            drawOps[slot] = drawOp
            previousDrawOp = drawOp
            checksum = checksum xor drawOp.color xor measured.baseline
        }

        for (index in recomposedGroups.indices step 17) {
            checksum += recomposedGroups[index]!!.node.dirtyVersion
        }
        for (index in drawOps.indices step 73) {
            checksum += drawOps[index]!!.measureResult.width
        }
        return checksum
    }

    private fun newNode(depth: Int, allNodes: MutableList<ComposeLikeNode>): ComposeLikeNode {
        val id = allNodes.size
        val children = arrayOfNulls<ComposeLikeNode>(COMPOSE_LIKE_CHILDREN_PER_NODE)
        val node = ComposeLikeNode(
            id,
            children,
            ComposeLikeState(version = 0, value = id),
            newModifierChain(id),
        )
        allNodes.add(node)
        if (depth < COMPOSE_LIKE_TREE_DEPTH) {
            for (index in children.indices) {
                children[index] = newNode(depth + 1, allNodes)
            }
        }
        return node
    }

    private fun newModifierChain(nodeId: Int): ComposeLikeModifier? {
        var head: ComposeLikeModifier? = null
        repeat(COMPOSE_LIKE_MODIFIERS_PER_NODE) { index ->
            head = ComposeLikeModifier(index, nodeId * 31 + index, head)
        }
        return head
    }

    private fun indexFor(frame: Int, slot: Int, stride: Int): Int {
        return ((frame * 1103 + slot * stride) and Int.MAX_VALUE) % nodes.size
    }

    private fun changeMaskFor(node: ComposeLikeNode): Int {
        return foldModifiers(node.modifier, node.state.version xor node.id) xor node.state.value
    }

    private fun measure(node: ComposeLikeNode, input: ComposeLikeMeasureInput): ComposeLikeMeasureResult {
        var nonNullChildren = 0
        for (child in node.children) {
            if (child != null) {
                nonNullChildren += 1
            }
        }
        val modifierWeight = foldModifiers(node.modifier, input.maxWidth + input.maxHeight)
        val width = input.maxWidth + (modifierWeight and 15)
        val height = input.maxHeight + nonNullChildren * input.density
        return ComposeLikeMeasureResult(node, width, height, baseline = height / 2 + (node.id and 7))
    }

    private fun foldModifiers(modifier: ComposeLikeModifier?, seed: Int): Int {
        var current = modifier
        var result = seed
        while (current != null) {
            result = result * 31 + current.kind + current.weight
            current = current.next
        }
        return result
    }
}

private fun scheduleAndWaitGcAfterFrame(): Int {
    val collectionsBefore = GC.edenCollectionsCount + GC.fullCollectionsCount
    GC.schedule()
    var spins = 0
    while (GC.edenCollectionsCount + GC.fullCollectionsCount == collectionsBefore && spins < COMPOSE_LIKE_GC_WAIT_SPINS) {
        Worker.current.park(COMPOSE_LIKE_IDLE_AFTER_GC_SCHEDULE_MICROS, process = false)
        spins += 1
    }
    return spins
}

@State(Scope.Benchmark)
@Measurement(time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
class ComposeLikeGCChurnBenchmark : SkipWhenBaseOnly() {
    private val fullGrowthTriggerPercent = GC.fullGrowthTriggerPercent
    private val scene = ComposeLikeScene()

    init {
        GC.fullGrowthTriggerPercent = COMPOSE_LIKE_FULL_GROWTH_TRIGGER_PERCENT
        GC.collect()
    }

    @Benchmark
    fun composeLikeFramesWithLargeStableUiTree(bh: Blackhole) {
        skipWhenBaseOnly()

        var checksum = 0
        repeat(COMPOSE_LIKE_FRAMES_PER_OPERATION) { frame ->
            checksum = checksum xor scene.composeFrame()
            if ((frame + 1) % COMPOSE_LIKE_GC_SCHEDULE_PERIOD == 0) {
                checksum += scheduleAndWaitGcAfterFrame()
            }
        }
        bh.consume(checksum)
    }

    @TearDown
    fun restoreGcPolicy() {
        GC.fullGrowthTriggerPercent = fullGrowthTriggerPercent
    }
}
