/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package hair.utils

import hair.graph.InequalityGraph
import hair.inequality.Bound
import hair.inequality.MinVertex
import hair.inequality.PhiVertex
import hair.inequality.Vertex
import hair.inequality.VertexKey
import hair.inequality.keyOf
import hair.ir.Session
import hair.ir.nodes.ArrayIndexCheck
import hair.ir.nodes.index

private const val upperColor = "black"
private const val lowerColor = "#087CFA"
private const val phiFill = "#DAE8FC"
private const val synthesizedFill = "#FFF2CC"
private const val zeroFill = "#EEEEEE"
private const val entryColor = "B85450"

fun InequalityGraph.toDot(
    describe: (VertexKey) -> String? = { null },
    entries: Map<VertexKey, String> = emptyMap(),
): String = buildString {
    appendLine("digraph InequalityGraph {")
    appendLine("  rankdir=TB;")
    appendLine("  ranksep=0.5;")
    appendLine("  nodesep=0.25;")
    appendLine("  node [shape=box, fontname=\"monospace\"];")
    appendLine("  edge [fontname=\"monospace\"];")

    for (v in vertices) {
        val lines = buildList {
            val desc = describe(v.key)
            if (v.key !is VertexKey.Value || desc == null) add(v.name)
            desc?.let(::add)
            entries[v.key]?.let { add("[$it]") }
        }
        val attrs = buildList {
            add("label=\"${lines.joinToString("\\n") { it.escaped() }}\"")
            when {
                v.key == VertexKey.Zero && !(v as MinVertex).isSynthesized -> add("style=filled, fillcolor=\"${zeroFill}\"")
                v is MinVertex && v.isSynthesized -> add("style=\"filled,dashed\", fillcolor=\"$synthesizedFill\"")
                v is PhiVertex -> add("style=filled, fillcolor=\"$phiFill\"")
            }
            if (v.key in entries) add("color=\"#$entryColor\", penwidth=2, peripheries=2")
        }
        appendLine("  ${v.dotId} [${attrs.joinToString(", ")}];")
    }

    for (v in vertices) {
        for (bound in Bound.entries) {
            val [color, style] = when (bound) {
                Bound.UPPER -> upperColor to "solid"
                Bound.LOWER -> lowerColor to "dashed"
            }
            for (e in v.preds(bound)) {
                appendLine(
                    "  ${e.from.dotId} -> ${v.dotId} [label=\"${e.weight}\", color=\"$color\", fontcolor=\"$color\", style=$style];"
                )
            }
        }
    }
    appendLine("}")
}

fun Session.inequalityGraphLabels(): (VertexKey) -> String? {
    val byId = allNodes().associateBy { it.id }
    return { key ->
        when (key) {
            is VertexKey.Value -> byId[key.nodeId]?.toString()
            is VertexKey.Length -> byId[key.arrayId]?.let { "length of $it" }
            is VertexKey.Zero -> null
        }
    }
}

fun Session.checkEntries(): Map<VertexKey, String> =
    allNodes<ArrayIndexCheck>()
        .groupBy { keyOf(it.index) }
        .mapValues { it -> "index of check " + it.value.joinToString(", ") { "#${it.id}" } }

private val Vertex.dotId: String get() = "\"${name.escaped()}\""

private fun String.escaped() = replace("\\", "\\\\").replace("\"", "\\\"")
