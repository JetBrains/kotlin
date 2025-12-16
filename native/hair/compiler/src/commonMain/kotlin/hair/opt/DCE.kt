/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package hair.opt

import hair.graph.dfs
import hair.ir.*
import hair.ir.nodes.*
import hair.ir.spine
import hair.utils.forEachInWorklist
import hair.utils.isEmpty

fun Session.eliminateDead(): Boolean {
    return eliminateDeadBlocks() || eliminateDeadFoam()
}

fun Session.eliminateDeadBlocks(): Boolean {
    val alive = dfs(cfg()).toList()
    val dead = allNodes<BlockEntry>().filter { it !in alive }.toList()
    if (dead.isEmpty()) return false
    modifyIR {
        for (block in dead) {
            if (!block.registered) continue
            block.nextOrNull?.let {
                it.control = unreachable
            }
            block.replaceValueUsesAndKill(NoValue())
        }
    }
    return true
}

fun Session.eliminateDeadFoam(): Boolean {
    var changed = false
    forEachInWorklist(allNodes()) { node ->
        // FIXME what about cyclic dependencies?
        // FIXME maybe find common grounds for control flow handling
        if (node !is ControlFlow && node.uses.isEmpty()) {
            // FIXME fix this registered/deregistered mess
            if (node.registered) {
                changed = true
                addAll(node.args.filterNotNull())
                node.deregister()
            }
        }
    }
    return changed
}