/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package hair.graph

import hair.compilation.FunctionCompilation
import hair.inequality.*
import hair.ir.NodeVisitor
import hair.ir.Session
import hair.ir.nodes.*
import hair.ir.type
import hair.sym.CmpOp
import hair.sym.HairType
import hair.transform.valueType
import hair.transform.withValueTypes
import hair.utils.checkEntries
import hair.utils.inequalityGraphLabels
import hair.utils.toDot

class InequalityGraph internal constructor() {
    private val index = LinkedHashMap<VertexKey, Vertex>()

    val vertices: Collection<Vertex> get() = index.values

    operator fun get(key: VertexKey): Vertex? = index[key]

    fun vertexOf(node: Node): Vertex? = index[keyOf(node)]
    fun lengthOf(array: Node): Vertex? = index[VertexKey.Length(array.id)]
    val zero: Vertex? get() = index[VertexKey.Zero]

    internal fun add(v: Vertex) {
        check(index.put(v.key, v) == null) { "duplicate vertex ${v.key}" }
    }
}

class InequalityGraphBuilder private constructor(nodes: Sequence<Node>) : NodeVisitor<Unit>() {
    private val graph = InequalityGraph()

    private val piIndex: Map<Pair<Node, Node>, Pi> = buildMap {
        for (pi in nodes.filterIsInstance<Pi>()) {
            putIfAbsent(pi.origin to pi.value, pi)
        }
    }

    private fun piOf(origin: Node, value: Node): Pi? = piIndex[origin to value]

    private fun vertexFor(node: Node): Vertex {
        val key = keyOf(node)
        graph[key]?.let { existing ->
            if (existing is MinVertex) existing.materialize()
            return existing
        }
        val v = if (node is Phi) PhiVertex(key as VertexKey.Value) else MinVertex(key, false)
        register(v)
        if (node is Const && node.type.isIntegral && key != VertexKey.Zero) {
            equal(v, zero(), node.value.toLong())
        }
        return v
    }

    private fun zero(): Vertex =
        graph[VertexKey.Zero] ?: MinVertex(VertexKey.Zero, true).also(::register)

    private fun lengthOf(array: Node): Vertex {
        val key = VertexKey.Length(array.id)
        return graph[key] ?: MinVertex(key, true).also(::register)
    }

    private fun register(v: Vertex) {
        graph.add(v)
        if (v.key is VertexKey.Length) lower(zero(), v, 0)
    }

    /** Upper: `to <= from + w` */
    private fun upper(from: Vertex, to: Vertex, w: Long) = to.addPred(Bound.UPPER, from, w)

    /** Lower: `to >= from + w` */
    private fun lower(from: Vertex, to: Vertex, w: Long) = to.addPred(Bound.LOWER, from, w)

    /** `to == from + c` only stores forward relations in a flow.
     *
     *  For π(x), the π node inherits every bound on x, while x inherits nothing from π.*/
    private fun flow(from: Vertex, to: Vertex, c: Long) {
        upper(from, to, c)
        lower(from, to, c)
    }

    /** `a == b + c` for values that equal */
    private fun equal(a: Vertex, b: Vertex, c: Long) {
        flow(b, a, c)
        flow(a, b, -c)
    }

    override fun visitNode(node: Node) = Unit

    override fun visitConst(node: Const) {
        if (node.type.isIntegral) vertexFor(node)
    }

    // C1
    override fun visitArraySize(node: ArraySize) {
        vertexFor(node)
    }

    override fun visitNewArray(node: NewArray) {
        val size = vertexFor(node.size)
        if (size is PhiVertex) return
        upper(lengthOf(node), size, 0)
    }

    // C3
    override fun visitAdd(node: Add) {
        if (!node.opType.toHairType().isTracked) return
        val c = (node.rhs as? Const)?.value?.toLong() ?: return
        flow(vertexFor(node.lhs), vertexFor(node), c)
    }

    override fun visitSub(node: Sub) {
        if (!node.opType.toHairType().isTracked) return
        val c = (node.rhs as? Const)?.value?.toLong() ?: return
        flow(vertexFor(node.lhs), vertexFor(node), -c)
    }

    override fun visitPhi(node: Phi) {
        if (!node.valueType.isTracked) return
        val v = vertexFor(node)
        for (arg in node.joinedValues) flow(vertexFor(arg), v, 0)
    }

    // C4
    override fun visitIfProjection(node: IfProjection) {
        val cmp = node.owner.cond as? Cmp ?: return
        if (!cmp.type.isTracked) return
        val op = if (node is TrueExit) cmp.op else cmp.op.negated()
        val a = Operand(node, cmp.lhs)
        val b = Operand(node, cmp.rhs)
        when (op) {
            CmpOp.EQ -> {
                scopedFact(a, b, 0); scopedFact(b, a, 0)
            }
            CmpOp.NE -> Unit

            CmpOp.S_LT -> scopedFact(a, b, -1)
            CmpOp.S_LE -> scopedFact(a, b, 0)
            CmpOp.S_GT -> scopedFact(b, a, -1)
            CmpOp.S_GE -> scopedFact(b, a, 0)

            // TODO: check big unsigned values
            CmpOp.U_LT -> if (cmp.rhs is ArraySize) {
                scopedFact(a, b, -1)
                scopedNonNegative(a)
            }
            CmpOp.U_LE -> if (cmp.rhs is ArraySize) {
                scopedFact(a, b, 0)
                scopedNonNegative(a)
            }
            CmpOp.U_GT -> if (cmp.lhs is ArraySize) {
                scopedFact(b, a, -1)
                scopedNonNegative(b)
            }
            CmpOp.U_GE -> if (cmp.lhs is ArraySize) {
                scopedFact(b, a, 0)
                scopedNonNegative(b)
            }
        }
    }

    // C5
    override fun visitArrayIndexCheck(node: ArrayIndexCheck) {
        val pi = piOf(node, node.index) ?: return
        val p = vertexFor(pi)
        upper(lengthOf(node.array), p, -1)
        lower(zero(), p, 0)
    }

    override fun visitPi(node: Pi) {
        if (!node.valueType.isTracked) return
        flow(vertexFor(node.value), vertexFor(node), 0)
    }

    private inner class Operand(exit: IfProjection, value: Node) {
        private val pi = piOf(exit, value)
        val vertex: Vertex = vertexFor(pi ?: value)
        val isScoped: Boolean get() = pi != null
    }

    private fun scopedFact(a: Operand, b: Operand, c: Long) {
        if (a.isScoped) upper(b.vertex, a.vertex, c)
        if (b.isScoped) lower(a.vertex, b.vertex, -c)
    }

    private fun scopedNonNegative(x: Operand) {
        if (x.isScoped) lower(zero(), x.vertex, 0)
    }

    companion object {
        fun build(nodes: Sequence<Node>): InequalityGraph {
            val builder = InequalityGraphBuilder(nodes)
            for (node in nodes) node.accept(builder)
            return builder.graph
        }
    }
}

private val HairType.isTracked get() = this == HairType.INT

// TODO: move to CmpOp.kt?
private fun CmpOp.negated(): CmpOp = when (this) {
    CmpOp.EQ -> CmpOp.NE
    CmpOp.NE -> CmpOp.EQ
    CmpOp.S_LT -> CmpOp.S_GE
    CmpOp.S_GE -> CmpOp.S_LT
    CmpOp.S_GT -> CmpOp.S_LE
    CmpOp.S_LE -> CmpOp.S_GT
    CmpOp.U_LE -> CmpOp.U_GT
    CmpOp.U_GT -> CmpOp.U_LE
    CmpOp.U_GE -> CmpOp.U_LT
    CmpOp.U_LT -> CmpOp.U_GE
}

fun Session.buildInequalityGraph(): InequalityGraph =
    InequalityGraphBuilder.build(allNodes())

context(compilation: FunctionCompilation)
inline fun <T> Session.withInequalityGraph(action: context(InequalityGraph) () -> T): T =
    withValueTypes {
        val graph = buildInequalityGraph()
        compilation.dumpHairRaw("inequality_graph") { graph.toDot(inequalityGraphLabels(), checkEntries()) }
        context(graph) {
            action()
        }
    }
