/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package hair.opt

import hair.graph.dfs
import hair.ir.*
import hair.ir.nodes.*
import hair.utils.forEachInWorklist

fun Session.eliminateDead(): Boolean {
    // Two phases run back to back:
    //   1. remove blocks that are unreachable in the CFG, and
    //   2. sweep away the dead "foam" — value nodes not reachable from any control-flow root
    //      (including whatever the block removal just orphaned).
    val aliveBlocks = dfs(cfg()).toList()
    val deadBlocks = allNodes<BlockEntry>().filter { it !in aliveBlocks }.toList()
    if (deadBlocks.isNotEmpty()) {
        modifyIR(runDCE = false) {
            for (block in deadBlocks) {
                if (!block.registered) continue
                block.nextOrNull?.let {
                    it.control = unreachable
                }
                block.replaceValueUsesAndKill(NoValue())
            }
        }
    }
    return eliminateDeadFoam() || deadBlocks.isNotEmpty()
}

private fun Session.eliminateDeadFoam(): Boolean {
    // Mark-and-sweep to handle dead cycles (e.g. mutually referencing Phis)
    // FIXME maybe find common grounds for control flow handling
    val alive = mutableSetOf<Node>()
    forEachInWorklist(allNodes().filter { it is ControlFlow }) { node ->
        if (alive.add(node)) addAll(node.args.filterNotNull())
    }

    val dead = allNodes().filter { it.registered && it !is ControlFlow && it !in alive }.toList()
    if (dead.isEmpty()) return false

    // The dead set is closed under uses: every user of a dead node is itself dead.
    // So we first sever all argument edges leaving the set,
    // which empties every dead node's use list even across cycles, and only then deregister.
    for (node in dead) {
        for (arg in node.args) arg?.removeUse(node)
    }
    for (node in dead) {
        node.deregister()
    }
    return true
}
