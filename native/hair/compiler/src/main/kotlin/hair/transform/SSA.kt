package hair.transform

import hair.graph.Dominators
import hair.ir.Session
import hair.ir.*
import hair.ir.nodes.*
import hair.transform.*
import hair.utils.closure
import hair.utils.shouldNotReachHere

fun Session.buildSSA() {
    val cfg = cfg()
    val doms = Dominators.sfda(cfg)

    modifyIR {
        val defs = allNodes<AssignVar>().groupBy { it.variable }
        for ((variable, assigns) in defs) {
            val assignBlocks = assigns.map { it }.toSet()
            val redefBlocks = assignBlocks.flatMap { doms.dominanceFrontier(it) }.toSet().closure {
                doms.dominanceFrontier(it).toList()
            }
            for (block in redefBlocks) {
                val entersCount = when (block) {
                    is Block -> block.enters.count()
                    is CatchBlock -> block.throwers.count()
                    else -> shouldNotReachHere(block)
                }
                Placeholder(variable)(block, *(Array(entersCount) { NoValue() }))
            }
        }

        fun search(block: ControlFlow, lastDef0: Map<Var, Node>) {
            val lastDef1 = lastDef0.toMutableMap()
            for (ph in block.uses.filterIsInstance<Placeholder>()) {
                lastDef1[ph.tag as Var] = ph
            }
            // FIXME is it correct? Recheck everything here!
            val toRemove = mutableListOf<VarOp>()
            for (n in listOf(block)) {
                when (n) {
                    is AssignVar -> {
                        lastDef1[n.variable] = n.assignedValue
                        n.replaceValueUses(NoValue()) // FIXME temporal hack to workaround lastLocationAccess
                        toRemove += n
                    }

                    is ReadVar -> {
                        val replacement = lastDef1[n.variable]!!
                        n.replaceValueUses(replacement)
                        toRemove += n
                    }

                    else -> {}
                }
            }
            for (nextBlock in cfg.succs(block)) {
                val blockEdgeIndex =
                    cfg.preds(nextBlock).withIndex().filter { it.value == block }.map { it.index }.single()
                for (ph in nextBlock.uses.filterIsInstance<Placeholder>()) {
                    val phiEdgeIndex = blockEdgeIndex + 1 // skip block
                    ph.inputs[phiEdgeIndex] = lastDef1[ph.tag as Var]!!
                }
            }
            for (child in doms.tree.succs(block)) {
                search(child, lastDef1)
            }
            for (n in toRemove) {
                n.removeFromControl()
            }
        }

        search(entryBlock, emptyMap())

        val phiPlaceholders = allNodes().filterIsInstance<Placeholder>().toList()
        for (ph in phiPlaceholders) {
            val block = ph.inputs[0] as Block
            // FIXME a tool needed
            val joinedValues = ph.inputs.drop(1)
            if (ph.uses.isEmpty()) {
                // FIXME should happen automatically
                ph.deregister()
            } else {
                val phi = Phi(block, *joinedValues.toTypedArray())
                ph.replaceValueUses(phi)
            }
        }
    }
}

fun Node.replaceValueUsesByNewVar(varName: String? = null): Var {
    val node = this
    val variable = varName?.let { Var(it) } ?: Var.nextNumbered()
    for (use in uses.toList()) {
        with (session as Session) {
            modifyIR {
                val by by lazy {
                    withGCM { gcm ->
                        val before = when (use) {
                            is Phi -> use.inputs[node]!!
                            is Controlled -> use
                            else -> {
                                gcm.pos(use) as Controlled
                            }
                        }
                        insertBefore(before) { ReadVar(variable) }
                    }
                }
                // FIXME copy&paste
                val useEdges = use.args.toList().withIndex().filter { it.value == node }
                for ((argIndex, _) in useEdges) {
                    use.args[argIndex] = by
                }
            }
        }
    }

    with (session as Session) {
        withGCM { gcm ->
            insertAfter(gcm.pos(node)) {
                AssignVar(variable)(node)
            }
        }
    }

    return variable
}
