package hair.transform

import hair.graph.*
import hair.ir.Session
import hair.ir.*
import hair.ir.nodes.*
import hair.utils.*

// FIXME beware that GCM can be invalidated!!!!!!!!!!

// FIXME make thread-safe
private val activeGCM = mutableMapOf<Session, GCMResult>()

fun <T> Session.withGCM(action: NodeBuilder.(GCMResult) -> T): T = withGCMImpl { gcm ->
    modifyIR { action(gcm) }
}

fun <T> Session.withGCMImpl(action: (GCMResult) -> T): T {
    val existingGcmResult = activeGCM[this]
    if (existingGcmResult != null) {
        return action(existingGcmResult)
    } else {
        val gcmResult = performGCM(this)
        activeGCM[this] = gcmResult
        val result = action(gcmResult)
        activeGCM.remove(this)
        return result
    }
}

fun performGCM(session: Session): GCMResult {
    // 1. Find the CFG dominator tree, and annotate basic blocks with the dominator tree depth.
    val cfg = session.cfg()
    val dominators = Dominators.sfda(cfg)
    val domDepth = run {
        val result = mutableMapOf<ControlFlow, Int>(session.entryBlock to 0)
        for (block in dfs(dominators.tree)) {
            result[block] = result[dominators.idom(block)]!! + 1
        }
        result.asTotalMap()
    }
    // 2. Find loops and compute loop nesting depth for each basic block.
    // TODO loop nesting
    // 3, Schedule (select basic blocks for) all instructions early, based on existing control and data dependence.
    //    We place instructions in the first block where they are dominated by their inputs.
    //    This schedule has a lot of speculative code, with extremely long live ranges.
//    val blockOfNodeCache = mutableMapOf<Node, Block>()
//    fun block(n: ControlFlow) = blockOfNodeCache.getOrPut(n) { n.block }

    val early = mutableMapOf<Node, ControlFlow>()
    // TODO generalize pinned nodes
    for (n in session.allNodes<ControlFlow>()) {
        early[n] = n
    }
    for (n in session.allNodes<Phi>()) {
        early[n] = n.block
    }
    fun scheduleEarly(n: Node) {
        if (n in early) return
        early[n] = session.entryBlock
        for (arg in n.args.filterNotNull()) {
            scheduleEarly(arg)
            // FIXME a lot of !!
            if (domDepth[early[n]!!] < domDepth[early[arg]!!]) {
                early[n] = early[arg]!!
            }
        }
    }
    for (n in session.allNodes<ControlFlow>()) {
        for (arg in n.args.filterNotNull()) {
            scheduleEarly(arg)
        }
    }
    for (n in session.allNodes<Phi>()) {
        for (arg in n.args.filterNotNull()) {
            scheduleEarly(arg)
        }
    }
    val late = mutableMapOf<Node, ControlFlow?>()
    for (n in session.allNodes<ControlFlow>()) {
        late[n] = n
    }
    for (n in session.allNodes<Phi>()) {
        late[n] = n.block
    }
    // 4. Schedule all instructions late. We place instructions in the last block where they dominate all their uses.
    fun scheduleLate(n: Node) {
        if (n in late) return

        var lca: ControlFlow? = null
        for (use in n.uses) {
            scheduleLate(use)
            val useBlock = when (use) {
                is Phi -> if (use.block == n) n else use.inputs[n]!!.block
                else -> late[use]
            }
            if (lca == null) {
                lca = useBlock
            } else if (useBlock != null) {
                lca = dominators.common(lca, useBlock)
            }
        }

        late[n] = lca
    }
    for (n in session.allNodes()) {
        scheduleLate(n)
    }
    // 5. Between the early schedule and the late schedule we have a safe range to place computations.
    //    We choose the block that is in the shallowest loop nest possible, and then is as control dependent as possible

    // FIXME kill dead nodes earlier?
    val bestBlock = session.allNodes().filter { it is ControlFlow || it.uses.isNotEmpty() }.associateWith {
        // TODO move out of loops etc.
        late[it] ?: early[it]!!
    }
    return GCMResult(bestBlock)
}

class GCMResult(val blocks: Map<Node, ControlFlow>) {
    fun block(n: Node): ControlFlow = blocks[n]!!

    val linearOrderCache = mutableMapOf<ControlFlow, List<Node>>()
    fun linearOrder(b: ControlFlow) = linearOrderCache.getOrPut(b) { linearize(b) }

    fun pos(node: Node): Controlling {
        val block = block(node)
        val order = linearOrder(block)
        // TODO do better
        var lastControl: Controlling? = null
        for (n in order) {
            if (n is Controlling) lastControl = n
            if (n == node) return lastControl!!
        }
        error("Should not reach here")
    }
}

fun GCMResult.linearize(block: ControlFlow): List<Node> {

    val blockNodes = blocks.entries.filter { it.value == block }.map { it.key }

    // TODO optimize
    val graph = object : DiGraph<Node> {
        override fun preds(n: Node): Sequence<Node> {
            val valueArgs = n.args.filterNotNull()
            val ctrl = when (n) {
                is Controlled -> n.prevControl
                else -> null
            }
            return (valueArgs.asSequence() + (ctrl?.let { sequenceOf(it) } ?: emptySequence())).filter { block(it) == block }
        }

        override fun succs(n: Node): Sequence<Node> {
            val valueUses = n.uses
            val ctrl = when (n) {
                is Controlling -> n.nextControl
                else -> null
            }
            return (valueUses.asSequence() + (ctrl?.let { sequenceOf(it) } ?: emptySequence())).filter { block(it) == block }
        }
    }

    val roots = mutableListOf<Node>()
    for (n in blockNodes) {
        if (graph.preds(n).count() == 0) {
            roots += n
        }
    }

    val postOrder = postOrder(roots, graph)

    val topSort = postOrder.toList().reversed()

//    println("GCM")
//    for ((n, b) in blocks) {
//        println("$n block $b")
//    }
//    println()
//    println("NODES")
//    for (n in blockNodes) {
//        println("$n preds: ${graph.preds(n).toList()} succs: ${graph.succs(n).toList()}")
//    }
//    println()
//    println("TS")
//    for (n in topSort) {
//        println("$n preds: ${graph.preds(n).toList()} succs: ${graph.succs(n).toList()}")
//    }
//    println()
    return topSort
}