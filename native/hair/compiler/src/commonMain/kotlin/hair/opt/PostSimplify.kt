/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package hair.opt

import hair.ir.*
import hair.ir.nodes.*

// TODO better name
context(_: NodeBuilder, _: ArgsUpdater, _: ControlFlowBuilder)
fun Session.simplify() {
    // TODO walk everything in use-def order
    val simplifier = object : NodeVisitor<Unit>() {
        override fun visitNode(node: Node) {}

        override fun visitBlockEntry(node: BlockEntry) {
            if (node == entry) return

            fun chooseReplacement(): Controlling? {
                // FIXME should always be killed by DCE?
                if (node.preds.all { it is Unreachable }) return unreachable

                val singleGotoPred = node.preds.singleOrNull() as? Goto
                if (singleGotoPred != null) {
                    val replacement = singleGotoPred.control
                    singleGotoPred.control = unreachable
                    return replacement
                }

                val unreachablePredIndexes = node.preds.withIndex().filter { it.value is Unreachable }.map { it.index }
                if (unreachablePredIndexes.isNotEmpty()) {
                    val reachablePreds = node.preds.withIndex().filter { it.index !in unreachablePredIndexes }.map { it.value }.toTypedArray()
                    val replacement = BlockEntry(*reachablePreds)
                    for (phi in node.phies.toList()) {
                        phi.replaceValueUsesAndKill(Phi(phi.type, replacement, *phi.inputs0.toTypedArray()))
                    }
                    return replacement
                }

                return null
            }

            chooseReplacement()?.let { replacement ->
                node.next.control = replacement
                node.kill()
            }
        }

        override fun visitGoto(node: Goto) {
            (node.control as? BlockEntry)?.let {
                val prevGoto = it.preds.singleOrNull() as? Goto
                if (prevGoto != null) {
                    it.preds[0] = unreachable
                    val next = node.next
                    // TODO a tool like node.replaceControlUsesAndKill(prevGoto)
                    next.preds.withIndex()
                        .filter { it.value == node }
                        .forEach { (index, _) ->
                            next.preds[index] = prevGoto
                        }
                    node.kill()
                }
            }
        }
    }

    for (node in allNodes().toList()) {
        if (!node.registered) continue
        node.accept(simplifier)
    }
}
