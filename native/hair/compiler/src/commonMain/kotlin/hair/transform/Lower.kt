/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package hair.transform

import hair.ir.*
import hair.ir.nodes.ArrayIndexCheck
import hair.ir.nodes.CheckCast
import hair.ir.nodes.IsInstanceOf
import hair.ir.nodes.Node
import hair.sym.CmpOp
import hair.sym.HairType
import hair.sym.RuntimeInterface
import hair.utils.withWorklist

// TODO still not usable
fun Session.lower() {
    withGCM {
        // FIXME new nodes will not be added to GCM!!!
        // either ensure that lowerable nodes are never created by lowering or BETTER: do incremental GCM
        // FIXME this hack precalculates positions for interesting nodes
        allNodes<IsInstanceOf>().forEach { pos(it) }

        withWorklist(allNodes()) {
            val worklist = this

            modifyControlFlow {
                // New nodes, e.g. those of a lowering sub-graph, are lowered as well.
                observingBuiltNodes({ worklist.add(it) }) {
                    val lowering = object : NodeVisitor<Unit>() {
                        override fun visitNode(node: Node) = Unit

                        override fun visitIsInstanceOf(node: IsInstanceOf) {
                            val control = pos(node)

                            // TODO fast type checks

                            // FIXME it's a nonthrowing runtime call - create a separate node  for such things
                            val replacement = control.insertAfter {
                                InvokeStatic(RuntimeInterface.isSubtype)(
                                    TypeInfo(node.obj),
                                    ConstTypeInfo(node.targetType)
                                )
                            }
                            node.replaceValueNodeWith(replacement)
                        }

                        override fun visitCheckCast(node: CheckCast) {
                            node.spliceOut(node.obj)
                        }

                        override fun visitArrayIndexCheck(node: ArrayIndexCheck) {
                            node.replaceWithSubGraph {
                                val size = ArraySize(node.array)
                                // Unsigned compare so a negative index (a huge unsigned value) also
                                // fails the upper-bound check with a single comparison.
                                branch(
                                    cond = Cmp(HairType.INT, CmpOp.U_GE)(node.index, size),
                                    trueInit = {
                                        InvokeStatic(RuntimeInterface.throwArrayIndexOutOfBounds)()
                                        breakControlFlowWithUnreachable()
                                    },
                                    falseInit = { },
                                )
                                null
                            }
                        }
                    }

                    for (node in worklist) {
                        if (!node.inGraph) continue
                        node.accept(lowering)
                    }
                }
            }
        }
    }
}
