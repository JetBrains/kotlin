/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package hair.inequality

import hair.ir.nodes.ArraySize
import hair.ir.nodes.Const
import hair.ir.nodes.Node
import hair.ir.nodes.array
import hair.ir.type

sealed interface VertexKey {
    data class Value(val nodeId: Int) : VertexKey
    data class Length(val arrayId: Int) : VertexKey
    data object Zero : VertexKey
}

fun keyOf(node: Node): VertexKey = when (node) {
    is ArraySize -> VertexKey.Length(node.array.id)
    is Const if node.type.isIntegral && node.value.toLong() == 0L -> VertexKey.Zero
    else -> VertexKey.Value(node.id)
}

enum class Bound { UPPER, LOWER }

class Edge(val from: Vertex, val weight: Long)

sealed class Vertex(val key: VertexKey) {
    private val upperIn = mutableListOf<Edge>()
    private val lowerIn = mutableListOf<Edge>()

    fun preds(bound: Bound): List<Edge> = when (bound) {
        Bound.UPPER -> upperIn
        Bound.LOWER -> lowerIn
    }

    internal fun addPred(bound: Bound, from: Vertex, weight: Long) {
        val list = if (bound == Bound.UPPER) upperIn else lowerIn
        if (list.none { it.from === from && it.weight == weight }) list += Edge(from, weight)
    }

    val name: String
        get() = when (val k = key) {
            is VertexKey.Value -> "n${k.nodeId}"
            is VertexKey.Length -> "len(n${k.arrayId})"
            VertexKey.Zero -> "zero"
        }

    override fun toString(): String = name
}

class MinVertex internal constructor(key: VertexKey, synthesized: Boolean) : Vertex(key) {
    var isSynthesized: Boolean = synthesized
        private set

    internal fun materialize() {
        isSynthesized = false
    }
}

class PhiVertex internal constructor(key: VertexKey) : Vertex(key)
