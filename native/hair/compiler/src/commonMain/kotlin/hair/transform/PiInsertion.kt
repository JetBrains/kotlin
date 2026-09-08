/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package hair.transform

import hair.ir.*
import hair.ir.nodes.*

fun Session.insertPis() {
    val narrowings = withGCMImpl {
        allNodes<If>().flatMap { ifNode ->
            narrowingsOf(ifNode)
        }.toList()
    }

    var count = 0
    val varNames = mutableMapOf<Node, Any>()
    for (info in narrowings) {
        for (v in info.values) {
            if (v.isNarrowable()) {
                varNames.getOrPut(v) {
                    if (v is ReadVar) v.variable else "pi$${count++}"
                }
            }
        }
    }

    for ([value, varName] in varNames) {
        if (value !is ReadVar) {
            value.replaceValueUsesByNewVar(varName)
        }
    }

    withGCM {
        for (info in narrowings) {
            insertBranchNarrowing(info, varNames)
        }
    }

    withGCM {
        for (aic in allNodes<ArrayIndexCheck>().toList()) {
            val index = aic.index
            if (!index.isNarrowable()) continue

            val varName: Any = if (index is ReadVar) {
                index.variable
            } else {
                val fresh = "pi$${count++}"
                index.replaceValueUsesByNewVar(fresh)
                fresh
            }

            val read = insertAfter(aic) { ReadVar(varName) as ReadVar }
            val pi = insertAfter(read) { Pi(read) as Pi }
            insertAfter(pi) { AssignVar(varName)(pi) as AssignVar }
        }
    }
}

private data class NarrowingInfo(val projection: IfProjection, val values: List<Node>)

private fun narrowingsOf(ifNode: If): List<NarrowingInfo> = when (val cond = ifNode.cond) {
    is Cmp -> {
        val vs = listOf(cond.lhs, cond.rhs)
        listOf(
            NarrowingInfo(ifNode.trueExit, vs),
            NarrowingInfo(ifNode.falseExit, vs),
        )
    }
    else -> emptyList()
}

context(_: NodeBuilder, _: ArgumentUpdater)
private fun Session.insertBranchNarrowing(
    info: NarrowingInfo,
    varNames: Map<Node, Any>
) {
    val names = info.values.mapNotNull { varNames[it] }.distinct()
    if (names.isEmpty()) return

    val branch: BlockEntry = info.projection.next
    if (branch.preds.singleOrNull() != info.projection) return

    var cursor: Controlling = branch
    for (varName in names) {
        val read = insertAfter(cursor) { ReadVar(varName) as ReadVar }
        val pi = insertAfter(read) { Pi(read) as Pi }
        cursor = insertAfter(pi) { AssignVar(varName)(pi) as AssignVar }
    }
}

private fun Node.isNarrowable(): Boolean =
    this !is Const && this !is Null && this !is True && this !is False

fun Node.replaceValueUsesByNewVar(varName: Any) {
    val node = this
    val originalUses = node.uses.toList()

    with(session) {
        val assignVar = withGCM {
            insertAfter(pos(node)) { AssignVar(varName)(node) as AssignVar }
        }

        for (use in originalUses) {
            val valueDrop = if (use is Controlled) 1 else 0
            val valueEdgeIndices = use.args.toList().withIndex()
                .drop(valueDrop)
                .filter { it.value == node }
                .map { it.index }
            if (valueEdgeIndices.isEmpty()) continue

            withGCM {
                val nodePos = pos(node)
                val usePos: Controlling = when (use) {
                    is Controlling -> use
                    else -> pos(use)
                }

                val readVar = if (usePos === nodePos || usePos === assignVar) {
                    insertAfter(assignVar) { ReadVar(varName) as ReadVar }
                } else {
                    val before: Controlled = when (use) {
                        is Phi -> when (val x = use.inputs.first { it.first == node }.second) {
                            is Unwind -> x.thrower as Controlled
                            is IfProjection -> x.owner
                            is Goto -> x
                            else -> error("Unexpected Phi input source: $x")
                        }
                        is Controlled -> use
                        else -> pos(use) as Controlled
                    }
                    insertBefore(before) { ReadVar(varName) as ReadVar }
                }
                for (argIndex in valueEdgeIndices) {
                    use.args[argIndex] = readVar
                }
            }
        }
    }
}
