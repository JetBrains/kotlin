package hair.transform

import hair.graph.Dominators
import hair.ir.Session
import hair.ir.*
import hair.ir.nodes.*
import hair.utils.closure
import hair.utils.indexOfSingle
import hair.utils.isEmpty

private typealias Var = Any

fun Session.buildSSA() {
    val cfg = cfg()
    val doms = Dominators.sfda(cfg)


    modifyIR {
        val defs = allNodes<AssignVar>().groupBy { it.variable }
        // FIXME find some general terms to do this?
        val handlersWIthPossibleRedefs = allNodes<Unwind>().map { it.handler }.filter { it.preds.size > 1 }.toSet()
        val phies = mutableMapOf<BlockEntry, MutableMap<Var, Phi>>()
        for ((variable, assigns) in defs) {
            val assignBlocks = assigns.map { it.block }.toSet()
            val redefBlocks = assignBlocks.flatMap { doms.dominanceFrontier(it) }.toSet().closure {
                doms.dominanceFrontier(it).toList()
            } + handlersWIthPossibleRedefs
            for (block in redefBlocks) {
                val blockPhies = phies.getOrPut(block) { mutableMapOf() }
                blockPhies[variable] = Phi(block, *arrayOfNulls(block.preds.size)) as Phi
            }
        }

        fun search(block: BlockEntry, domDefs: Map<Var, Node>) {
            val currentDefs = domDefs.toMutableMap()
            phies[block]?.let { currentDefs += it }
            fun patchInput(next: BlockEntry, blockExit: BlockExit) {
                val inputIndex = next.preds.indexOfSingle(blockExit)
                for ((variable, phi) in phies[next] ?: emptyMap()) {
                    phi.joinedValues[inputIndex] = currentDefs[variable]!!
                }
            }
            for (n in block.spine.toList()) {
                when (n) {
                    is AssignVar -> {
                        currentDefs[n.variable] = n.assignedValue
                        n.removeFromControl()
                    }

                    is ReadVar -> {
                        val replacement = currentDefs[n.variable]!!
                        n.replaceValueUses(replacement)
                        n.removeFromControl()
                    }

                    is Throwing -> {
                        val handler = n.unwind ?: continue
                        val handlerBlock = handler.handler
                        patchInput(handlerBlock, handler)
                    }

                    // FIXME generalize
                    is Goto -> n.uses.single().let {
                        patchInput(it as BlockEntry, n)
                    }
                    is Throw -> n.unwind?.handler?.let {
                        patchInput(it, n.unwind!!)
                    }
                    is If -> n.uses.forEach {
                        patchInput(it.uses.single() as BlockEntry, it as IfProjection)
                    }

                    else -> {}
                }
            }
            for (child in doms.tree.succs(block)) {
                search(child, currentDefs)
            }
        }

        search(entry, emptyMap())

        allNodes<Phi>().forEach { require(it.joinedValues.withNulls.all { it != null })}
    }
}

//fun Node.replaceValueUsesByNewVar(varName: String? = null): Var {
//    val node = this
//    val variable = varName?.let { Var(it) } ?: Var.nextNumbered()
//    for (use in uses.toList()) {
//        with (session) {
//            modifyIR {
//                val by by lazy {
//                    withGCM {
//                        val before: Controlled = when (use) {
//                            is Phi -> when (val x = use.inputs[node]!!) {
//                                is Unwind -> x.thrower as Controlled
//                                is IfProjection -> x.owner
//                                is Goto -> x
//                            }
//                            is Controlled -> use
//                            else -> pos(use) as Controlled
//                        }
//                        insertBefore(before) { ReadVar(variable) }
//                    }
//                }
//                // FIXME copy&paste
//                val useEdges = use.args.toList().withIndex().filter { it.value == node }
//                for ((argIndex, _) in useEdges) {
//                    use.args[argIndex] = by
//                }
//            }
//        }
//    }
//
//    with (session) {
//        withGCM {
//            insertAfter(pos(node)) {
//                AssignVar(variable)(node)
//            }
//        }
//    }
//
//    return variable
//}
