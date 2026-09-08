/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package hair.opt

import hair.compilation.FunctionCompilation
import hair.ir.nodes.*
import hair.ir.*
import hair.sym.CmpOp
import hair.sym.HairType
import hair.transform.withValueTypes

class InequalityGraph internal constructor(
    private val out: Map<Int, List<Edge>>,
    private val inn: Map<Int, List<Edge>>,
    private val phiJoined: Map<Int, IntArray>,
    private val syntheticSize: Map<Int, Int>
) {
    // source -> target: wright <=> target <= source + weight
    class Edge(val target: Int, val weight: Int)

    fun outgoingOf(vertex: Int): List<Edge> = out[vertex].orEmpty()
    fun incomingOf(vertex: Int): List<Edge> = inn[vertex].orEmpty()

    fun isPhi(vertex: Int): Boolean = vertex in phiJoined
    fun phiJoinedValues(vertex: Int): IntArray = phiJoined[vertex] ?: EMPTY_INTS

    fun vertexOf(node: Node): Int = node.id
    fun sizeVertexOf(array: Node): Int {
        array.uses.filterIsInstance<ArraySize>().firstOrNull()?.let { return it.id }
        return syntheticSize[array.id] ?: error("No size vertex for array ${array.id}")
    }

    fun renderDot(session: Session): String = buildString {
        val nodeById = session.allNodes().associateBy { it.id }
        val vertexIds = sortedSetOf<Int>().apply {
            add(ZERO)
            addAll(out.keys); addAll(inn.keys)
            addAll(phiJoined.keys)
            addAll(syntheticSize.values)
            for (list in out.values) for (e in list) add(e.target)
            for (list in inn.values) for (e in list) add(e.target)
            for (arr in phiJoined.values) for (v in arr) add(v)
        }

        fun esc(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"")
        val sizeVertToArr = syntheticSize.entries.associate { it.value to it.key }

        val indexEntryOf = HashMap<Int, MutableList<Int>>()
        for (n in nodeById.values) {
            if (n is ArrayIndexCheck) {
                indexEntryOf.getOrPut(vertexOf(n.index)) { mutableListOf() } += n.id
            }
        }

        appendLine("digraph InequalityGraph {")
        appendLine("  rankdir=TB;")
        appendLine("  ranksep=0.5;")
        appendLine("  nodesep=0.25;")
        appendLine("  node [shape=box, fontname=\"monospace\"];")
        appendLine("  edge [fontname=\"monospace\"];")
        for (v in vertexIds) {
            val label: String
            val extra: StringBuilder = StringBuilder()
            when {
                v == ZERO -> {
                    label = "ZERO"
                    extra.append(", style=filled, fillcolor=\"#eeeeee\"")
                }
                v in sizeVertToArr -> {
                    label = "syntheticSize\\narr#${sizeVertToArr[v]} (v=$v)"
                    extra.append(", style=filled, fillcolor=\"#fff2cc\"")
                }
                else -> {
                    val n = nodeById[v]
                    label = if (n != null) esc(n.toString()) else "v=$v"
                    if (v in phiJoined) extra.append(", style=filled, fillcolor=\"#dae8fc\"")
                }
            }
            val idxChecks = indexEntryOf[v]
            val fullLabel =
                if (idxChecks == null) label
                else label + "\\n[entry: index of check#${idxChecks.joinToString(",#")}]"
            if (idxChecks != null) extra.append(", color=\"#b85450\", penwidth=2, peripheries=2")
            appendLine("  n$v [label=\"$fullLabel\"$extra];")
        }
        for (entry in out.entries) {
            val u = entry.key
            for (e in entry.value) {
                appendLine("  n$u -> n${e.target} [label=\" ${e.weight} \"];")
            }
        }
        for (entry in phiJoined.entries) {
            val phi = entry.key
            for (j in entry.value) {
                appendLine("  n$j -> n$phi [style=dashed, color=\"#6c8ebf\", label=\" phi \"];")
            }
        }
        appendLine("}")
    }

    companion object {
        const val ZERO: Int = 0

        private val EMPTY_INTS = IntArray(0)

        context(_: FunctionCompilation)
        fun build(session: Session): InequalityGraph = session.withValueTypes {
            Builder().buildForm(session).freeze()
        }
    }
}

context(_: FunctionCompilation)
inline fun <T> Session.withInequalityGraph(action: (InequalityGraph) -> T): T =
    action(InequalityGraph.build(this))

private class Builder : NodeVisitor<Unit>() {
    private val out = HashMap<Int, MutableList<InequalityGraph.Edge>>()
    private val inn = HashMap<Int, MutableList<InequalityGraph.Edge>>()
    private val phiJoined = HashMap<Int, IntArray>()
    private val syntheticSize = HashMap<Int, Int>()
    private var nextSyntheticSize = -1

    fun buildForm(session: Session): Builder {
        for (n in session.allNodes()) {
            if (n.registered) n.accept(this)
        }
        return this
    }

    fun freeze(): InequalityGraph = InequalityGraph(out, inn, phiJoined, syntheticSize)

    override fun visitNode(node: Node) = Unit

    /** `v <= u + weight` becomes `u -> v : weight` */
    private fun addEdge(u: Int, v: Int, weight: Int) {
        out.getOrPut(u) { mutableListOf() } += InequalityGraph.Edge(v, weight)
        inn.getOrPut(v) { mutableListOf() } += InequalityGraph.Edge(u, weight)
    }

    private fun addEquality(a: Int, b: Int) {
        addEdge(a, b, 0)
        addEdge(b, a, 0)
    }

    override fun visitConst(node: Const) = whenIsIntTyped(node) {
        // v == c <=> v <= 0 + c ^ v >= 0 + c
        val c = node.value.toInt()
        addEdge(InequalityGraph.ZERO, node.id, c)
        addEdge(node.id, InequalityGraph.ZERO, -c)
    }

    override fun visitAdd(node: Add) = whenIsIntTyped(node) {
        // v = x + c <=> v <= x + c ^ v >= x + c
        val [x, c] = matchVarPlusConst(node.lhs, node.rhs) ?: return@whenIsIntTyped
        var cur: Node = x
        while (true) {
            addEdge(cur.id, node.id, c)
            addEdge(node.id, cur.id, -c)
            if (cur is Pi) cur = cur.value else break
        }
    }

    override fun visitSub(node: Sub) = whenIsIntTyped(node) {
        // v = x - c <=> v = x + (-c)
        // No constraint for v = c - x
        val rhs = node.rhs
        if (rhs !is Const) return@whenIsIntTyped
        val c = -rhs.value.toInt()
        addEdge(node.lhs.id, node.id, c)
        addEdge(node.id, node.lhs.id, -c)
    }

    override fun visitArraySize(node: ArraySize) {
        // arraySize(a) >= 0 <=> edge n -> zero : 0
        addEdge(node.id, InequalityGraph.ZERO, 0)
    }

    override fun visitPhi(node: Phi) = whenIsIntTyped(node) {
        val joined = node.joinedValues.map { it.id }.toIntArray()
        if (joined.isNotEmpty()) {
            phiJoined[node.id] = joined
            for (j in joined) {
                addEdge(node.id, j, 0)
            }
        }
    }

    override fun visitPi(node: Pi) {
        val value = node.value
        var probe: Node = value
        while (probe is Pi) probe = probe.value
        if ((probe as? NodeBase)?.valueTypeOrNull != HairType.INT) return

        // Pi = value
        addEquality(node.id, value.id)

        when (val src = findRefinementSource(node)) {
            is IfProjection -> refineFromBranch(node, value, src)
            is ArrayIndexCheck -> refineFromCheck(node, value, src)
            else -> Unit
        }
    }

    private fun whenIsIntTyped(node: Node, action: () -> Unit) {
        if (!node.isIntTyped()) return
        action()
    }

    private fun refineFromBranch(pi: Pi, value: Node, projection: IfProjection) {
        for (cmp in cmpsForBranch(projection)) {
            applyCmpRefinement(pi, value, cmp, projection)
        }
    }

    private fun applyCmpRefinement(pi: Pi, value: Node, cmp: Cmp, projection: IfProjection) {
        val taken = projection is TrueExit

        val [op, swap] = effectiveOp(cmp.op, taken) ?: return
        val lhs = if (swap) cmp.rhs else cmp.lhs
        val rhs = if (swap) cmp.lhs else cmp.rhs

        if (cmp.type != HairType.INT) return

        when {
            value === lhs -> emitPredicateEdges(pi.id, rhs.id, op, false)
            value === rhs -> emitPredicateEdges(pi.id, lhs.id, op, true)
        }
    }

    private fun cmpsForBranch(projection: IfProjection): List<Cmp> = when (val cond = projection.owner.cond) {
        is Cmp -> listOf(cond)
        is Phi -> shortCircuitCmps(cond, projection is TrueExit)
        else -> emptyList()
    }

    private fun shortCircuitCmps(phi: Phi, onTrueBranch: Boolean): List<Cmp> {
        val cmps = mutableListOf<Cmp>()
        var sawFalse = false
        var sawTrue = false
        for (j in phi.joinedValues) {
            when (j) {
                is Cmp -> cmps += j
                is False -> sawFalse = true
                is True -> sawTrue = true
                else -> return emptyList()
            }
        }
        if (cmps.isEmpty()) return emptyList()
        return when {
            onTrueBranch && sawFalse && !sawTrue -> cmps
            !onTrueBranch && sawTrue && !sawFalse -> cmps
            else -> emptyList()
        }
    }

    private fun emitPredicateEdges(piId: Int, otherId: Int, op: EffOp, refinedIsRhs: Boolean) {
        when (op) {
            EffOp.LT -> if (!refinedIsRhs) addEdge(otherId, piId, -1) else addEdge(piId, otherId, -1)
            EffOp.LE -> if (!refinedIsRhs) addEdge(otherId, piId, 0) else addEdge(piId, otherId, 0)
            EffOp.EQ -> addEquality(piId, otherId)
        }
    }

    private fun refineFromCheck(pi: Pi, value: Node, check: ArrayIndexCheck) {
        if (value !== check.index) return

        val size = sizeVertexOfBuilding(check.array)
        addEdge(pi.id, InequalityGraph.ZERO, 0) // 0 <= pi
        addEdge(size, pi.id, -1) // pi <= size - 1
    }

    private fun findRefinementSource(pi: Pi): Node? {
        var c: Node = pi.control
        while (true) {
            c = when (c) {
                is ArrayIndexCheck -> return c
                is Pi -> c.control
                is BlockEntry -> return c.preds.singleOrNull() as? IfProjection
                is Controlled -> c.control
                else -> return null
            }
        }
    }

    private fun matchVarPlusConst(lhs: Node, rhs: Node): Pair<Node, Int>? = when {
        rhs is Const -> lhs to rhs.value.toInt()
        lhs is Const -> rhs to lhs.value.toInt()
        else -> null
    }

    private fun sizeVertexOfBuilding(array: Node): Int {
        array.uses.filterIsInstance<ArraySize>().firstOrNull()?.let { return it.id }
        return syntheticSize.getOrPut(array.id) {
            val id = nextSyntheticSize--
            addEdge(id, InequalityGraph.ZERO, 0)
            id
        }
    }

    private fun Node.isIntTyped(): Boolean =
        (this as? NodeBase)?.valueTypeOrNull == HairType.INT
}

private enum class EffOp { LT, LE, EQ }

private fun effectiveOp(op: CmpOp, taken: Boolean): Pair<EffOp, Boolean>? = when (op) {
    CmpOp.S_LT -> if (taken) EffOp.LT to false else EffOp.LE to true
    CmpOp.S_LE -> if (taken) EffOp.LE to false else EffOp.LT to true
    CmpOp.S_GT -> if (taken) EffOp.LT to true else EffOp.LE to false
    CmpOp.S_GE -> if (taken) EffOp.LE to true else EffOp.LT to false
    CmpOp.EQ -> if (taken) EffOp.EQ to false else null
    CmpOp.NE -> if (taken) null else EffOp.EQ to false

    // TODO: unsigned?
    CmpOp.U_LT, CmpOp.U_LE, CmpOp.U_GT, CmpOp.U_GE -> null
}
