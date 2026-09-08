/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package hair.opt

import hair.compilation.FunctionCompilation
import hair.graph.Dominators
import hair.ir.*
import hair.ir.nodes.*

class BoundsProver(
    private val graph: InequalityGraph,
    private val excludedVertices: Set<Int> = emptySet(),
) {
    fun proveLessThanSize(idx: Node, array: Node): Boolean =
        // idx <= size(array) - 1
        demandProve(graph.sizeVertexOf(array), graph.vertexOf(idx), -1)

    fun proveNonNegative(idx: Node): Boolean =
        // 0 <= idx + 0
        demandProve(graph.vertexOf(idx), InequalityGraph.ZERO, 0)

    private enum class L {
        True, False, Reduced;

        infix fun meet(other: L) = when {
            this == False || other == False -> False
            this == Reduced || other == Reduced -> Reduced
            else -> True
        }

        infix fun join(other: L) = when {
            this == True || other == True -> True
            this == Reduced || other == Reduced -> Reduced
            else -> False
        }
    }

    private fun demandProve(source: Int, target: Int, c: Int): Boolean {
        val memo = HashMap<Int, VertexMemo>()
        val active = HashMap<Int, Int>()
        return prove(source, target, c, memo, active, false) != L.False
    }

    private fun prove(
        source: Int,
        v: Int,
        c: Int,
        memo: HashMap<Int, VertexMemo>,
        active: HashMap<Int, Int>,
        sawPhi: Boolean,
    ): L {
        if (v in excludedVertices) return L.False

        memo[v]?.query(c)?.let { return it }

        if (v == source && c >= 0) return L.True

        val isPhi = graph.isPhi(v)

        val hasPred =
            if (isPhi) graph.phiJoinedValues(v).any { it !in excludedVertices}
            else graph.incomingOf(v).any { it.target !in excludedVertices }
        if (!hasPred) return L.False

        active[v]?.let { entered ->
            return when {
                c >= entered -> L.False
                sawPhi -> L.Reduced
                else -> L.False
            }
        }

        active[v] = c
        val nextSawPhi = sawPhi || isPhi
        var acc: L = if (isPhi) L.True else L.False

        if (isPhi) {
            for (j in graph.phiJoinedValues(v)) {
                val sub =
                    if (j in excludedVertices) L.False
                    else prove(source, j, c, memo, active, nextSawPhi)
                acc = acc meet sub
                if (acc == L.False) break
            }
        } else {
            for (edge in graph.incomingOf(v)) {
                if (edge.target in excludedVertices) continue
                val sub = prove(source, edge.target, c - edge.weight, memo, active, nextSawPhi)
                acc = acc join sub
                if (acc == L.True) break
            }
        }

        active.remove(v)

        if (acc == L.True || acc == L.False) {
            memo.getOrPut(v) { VertexMemo() }.record(c, acc)
        }
        return acc
    }

    private class VertexMemo {
        private var provenFrom: Int = Int.MAX_VALUE
        private var disprovedTo: Int = Int.MIN_VALUE

        fun query(c: Int): L? = when {
            provenFrom <= c -> L.True
            disprovedTo >= c -> L.False
            else -> null
        }

        fun record(c: Int, result: L) {
            when (result) {
                L.True -> if (c < provenFrom) provenFrom = c
                L.False -> if (c > disprovedTo) disprovedTo = c
                L.Reduced -> Unit
            }
        }
    }
}

context(fc: FunctionCompilation)
fun Session.eliminateBoundsChecks() {
    withInequalityGraph { graph ->
        fc.dumpHairRaw("inequality_graph", graph.renderDot(this))
        val doms = Dominators.sfda(cfg())
        val allPis = allNodes<Pi>().toList()
        modifyIR {
            for (check in allNodes<ArrayIndexCheck>().toList()) {
                if (canEliminate(check, graph, doms, allPis)) {
                    check.removeFromControl()
                }
            }
        }
    }
}


private fun canEliminate(
    check: ArrayIndexCheck,
    graph: InequalityGraph,
    doms: Dominators<BlockEntry>,
    allPis: List<Pi>,
): Boolean {
    if (check.unwind != null) return false

    val checkBlock = check.block
    val excluded = buildSet {
        check.uses.filterIsInstance<Pi>().forEach { add(it.id) }
        for (pi in allPis) {
            if (!doms.dominates(pi.block, checkBlock)) add(pi.id)
        }
    }

    val prover = BoundsProver(graph, excluded)
    val a = prover.proveNonNegative(check.index)
    val b = prover.proveLessThanSize(check.index, check.array)
    return a && b
}
