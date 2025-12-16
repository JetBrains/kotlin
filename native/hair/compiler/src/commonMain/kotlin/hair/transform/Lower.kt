/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package hair.transform

import hair.ir.*
import hair.ir.nodes.ControlFlowBuilder
import hair.ir.nodes.IsInstanceOf
import hair.ir.nodes.Node
import hair.ir.nodes.NodeBuilder
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

            // FIXME create a proper tool, maybe teach insertAfter to add to a worklist from context or smth.
            context(baseBuilder: NodeBuilder)
            fun addingToWorklist(action: context(NodeBuilder) () -> Unit) {
                val collectNewNodes = object : NodeBuilder by baseBuilder {
                    override fun onNodeBuilt(node: Node): Node {
                        return baseBuilder.onNodeBuilt(node).also {
                            worklist.add(it)
                        }
                    }
                }
                context(collectNewNodes) {
                    action()
                }
            }

            modifyControlFlow(unreachable) {
                addingToWorklist {
                    val lowering = object : NodeVisitor<Unit>() {
                        override fun visitNode(node: Node) = Unit

                        override fun visitIsInstanceOf(node: IsInstanceOf) {
                            contextOf<ControlFlowBuilder>().at(pos(node))

                            // TODO fast type checks

                            // FIXME it's a nonthrowing runtime call - create a separate node  for such things
                            val replacement = InvokeStatic(RuntimeInterface.isSubtype)(
                                TypeInfo(node.obj),
                                ConstTypeInfo(node.targetType)
                            )
                            node.replaceValueUsesAndKill(replacement)
                        }
                    }

                    for (node in worklist) {
                        node.accept(lowering)
                    }
                }
            }
        }
    }
}
