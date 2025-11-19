/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package hair.opt

import hair.ir.*
import hair.ir.nodes.*
import hair.ir.spine
import hair.utils.forEachInWorklist

context(_: NodeBuilder, _: ArgsUpdater)
fun Session.eliminateDead() {
//    forEachInWorklist(allNodes<BlockEntry>().filter { it != entry && it.preds.isEmpty }) {
//        // FIXME nulls?
//        if (it.registered && it.preds.withNulls.filterNotNull().isEmpty()) {
//            for (exit in it.exits.toList()) {
//                val next = exit.next
//                add(next)
//                for ((index, pred) in next.preds.withIndex()) {
//                    if (pred == exit) {
//                        next.preds[index] = null
//                    }
//                }
//            }
//            for (node in it.spine.toList().reversed()) {
//                if (node is If) {
//                    for (proj in node.uses.toList()) {
//                        proj.deregister()
//                    }
//                }
//                node.replaceValueUsesAndKill(NoValue())
//            }
//            it.deregister()
//        }
//    }
}