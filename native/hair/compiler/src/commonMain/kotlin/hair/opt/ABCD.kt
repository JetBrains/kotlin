/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package hair.opt

import hair.compilation.FunctionCompilation
import hair.graph.InequalityGraph
import hair.graph.withInequalityGraph
import hair.inequality.*
import hair.ir.Session
import hair.ir.modifyIR
import hair.ir.nodes.*
import hair.ir.removeFromControl

enum class Proof { FALSE, REDUCED, TRUE }

class DemandProver(private val bound: Bound) {
    private val sign = if (bound == Bound.UPPER) 1 else -1

    private val memo = HashMap<Vertex, HashMap<Long, Proof>>()

    private val path = ArrayList<Pair<Vertex, Long>>()
    private val onStack = HashMap<Vertex, Int>()

    fun prove(source: Vertex, target: Vertex, c: Long): Boolean {
        memo.clear()
        path.clear()
        onStack.clear()
        return search(source, target, sign * c) != Proof.FALSE
    }

    private fun search(a: Vertex, v: Vertex, c: Long): Proof {
        memo[v]?.let { m ->
            if (m.any { it.value == Proof.TRUE && it.key <= c }) return Proof.TRUE
            if (m.any { it.value == Proof.FALSE && it.key >= c }) return Proof.FALSE
            if (m.any { it.value == Proof.REDUCED && it.key <= c }) return Proof.REDUCED
        }
        if (v === a && c >= 0) return Proof.TRUE

        val predecessors = v.preds(bound)
        if (predecessors.isEmpty()) return Proof.FALSE

        onStack[v]?.let { pos -> return closeCycle(pos, c) }

        onStack[v] = path.size
        path += v to c

        var result = when (v) {
            is PhiVertex -> Proof.TRUE  // meet: every predecessor must hold
            is MinVertex -> Proof.FALSE // join: one predecessor suffices
        }
        for (e in predecessors) {
            val r = search(a, e.from, c - sign * e.weight)
            result = when (v) {
                is PhiVertex -> minOf(result, r)
                is MinVertex -> maxOf(result, r)
            }
            if (v is PhiVertex && result == Proof.FALSE) break
            if (v is MinVertex && result == Proof.TRUE) break
        }
        path.removeAt(path.lastIndex)
        onStack.remove(v)

        memo.getOrPut(v) { HashMap() }[c] = result
        return result
    }

    private fun closeCycle(pos: Int, c: Long): Proof {
        if (c < path[pos].second) return Proof.FALSE
        val throughPhi = (pos until path.size).any { path[it].first is PhiVertex }
        return if (throughPhi) Proof.REDUCED else Proof.FALSE
    }
}

fun InequalityGraph.isUpperRedundant(check: ArrayIndexCheck): Boolean {
    val idx = vertexOf(check.index) ?: return false
    val upper = DemandProver(Bound.UPPER)
    val lower = DemandProver(Bound.LOWER)
    for (len in listOfNotNull(vertexOf(check.length), lengthOf(check.array)).distinct()) {
        if (upper.prove(source = len, target = idx, c = -1)) return true
        // `if (constant < len) a[constant]` constant doesn't get a pi node (can't be narrowed), so we ask
        // is `pi(len) >= constant + 1` instead of the usual is `constant <= pi(len) - 1`
        if (lower.prove(source = idx, target = len, c = 1)) return true
    }
    return false
}

fun InequalityGraph.isLowerRedundant(check: ArrayIndexCheck): Boolean {
    val zero = zero ?: return false
    val idx = vertexOf(check.index) ?: return false
    return DemandProver(Bound.LOWER).prove(
        source = zero,
        target = idx,
        c = 0
    )
}

context(_: FunctionCompilation)
fun Session.eliminateBoundsCheck() {
    withInequalityGraph {
        val graph = contextOf<InequalityGraph>()
        val redundant = allNodes<ArrayIndexCheck>()
            .filter { graph.isUpperRedundant(it) && graph.isLowerRedundant(it) }
            .toList()
        removePis()
        modifyIR { for (check in redundant) check.removeFromControl() }
    }
}

context(_: FunctionCompilation)
fun Session.removePis() = modifyIR {
    for (pi in allNodes<Pi>().toList()) {
        pi.replaceValueUses(pi.value)
        pi.removeFromControl()
    }
}
