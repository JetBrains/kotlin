package hair.transform

import hair.graph.*
import hair.ir.Session
import hair.ir.*
import hair.ir.nodes.*
import hair.utils.*

// FIXME beware that GCM can be invalidated!!!!!!!!!!

class GCMResult(val blocks: Map<Node, BlockEntry>) {
    fun block(n: Node): BlockEntry = blocks[n] ?: error("No block computed for $n")

    val linearOrderCache = mutableMapOf<BlockEntry, List<Node>>()
    fun linearOrder(b: BlockEntry) = linearOrderCache.getOrPut(b) { linearize(b) }

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

context(gcm: GCMResult)
fun pos(n: Node): Controlling = gcm.pos(n)

// FIXME make thread-safe
private val activeGCM = mutableMapOf<Session, GCMResult>()

fun <T> Session.withGCM(action: context(GCMResult, NodeBuilder, ArgsUpdater) () -> T): T = withGCMImpl {
    // FIXME why modify IR? And how come here can be dying values at this point?
    // TODO require no dad nodes
    modifyIR { action() }
}

fun <T> Session.withGCMImpl(action: context(GCMResult) () -> T): T {
    val existingGcmResult = activeGCM[this]
    if (existingGcmResult != null) {
        return context(existingGcmResult) { action() }
    } else {
        val gcmResult = performGCM(this)
        activeGCM[this] = gcmResult
        val result = context(gcmResult) { action() }
        activeGCM.remove(this)
        return result
    }
}

fun performGCM(session: Session): GCMResult {
    // 1. Find the CFG dominator tree, and annotate basic blocks with the dominator tree depth.
    val cfg = session.cfg()
    val dominators = Dominators.sfda(cfg)
    val domDepth = run {
        val result = mutableMapOf<ControlFlow, Int>(session.entry to 0)
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

    val early = mutableMapOf<Node, BlockEntry>()
    // TODO generalize pinned nodes
    for (n in session.allNodes<ControlFlow>()) {
        early[n] = n.block
    }
    for (n in session.allNodes<Phi>()) {
        early[n] = n.block
    }
    fun scheduleEarly(n: Node) {
        if (n in early) return
        early[n] = session.entry
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
    val late = mutableMapOf<Node, BlockEntry?>()
    for (n in session.allNodes<ControlFlow>()) {
        late[n] = n.block
    }
    for (n in session.allNodes<Phi>()) {
        late[n] = n.block
    }
    // 4. Schedule all instructions late. We place instructions in the last block where they dominate all their uses.
    fun scheduleLate(n: Node) {
        if (n in late) return

        var lca: BlockEntry? = null
        for (use in n.uses) {
            scheduleLate(use) // FIXME this brakes on Placeholder nodes creating data-flow cycles
            val useBlock = when (use) {
                is Phi -> if (use.block == n) use.block else use.inputs[n]!!.block
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

fun GCMResult.linearize(block: ControlFlow): List<Node> {

    val blockNodes = blocks.entries.filter { it.value == block }.map { it.key }

    // TODO optimize
    val graph = object : DiGraph<Node> {
        override fun preds(n: Node): Sequence<Node> {
            return n.args.asSequence().filterNotNull().filter { block(it) == block }
        }

        override fun succs(n: Node): Sequence<Node> {
            return n.uses.filter { block(it) == block }
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
