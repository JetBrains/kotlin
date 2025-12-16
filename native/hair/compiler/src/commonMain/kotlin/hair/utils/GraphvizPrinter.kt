package hair.utils

import hair.graph.dfs
import hair.ir.*
import hair.ir.nodes.*
import hair.transform.*

private const val controlColor = "black"
private const val valueColor = "#FE2857"
private const val clusterColor = "#CDCDCD"
private const val projectionColor = "#087CFA"
private const val unorderedColor = "blue"

private const val controlShape = "rectangle"
private const val commonShape = "ellipse"
private const val blockExitShape = "diamond"

fun Session.generateGraphviz(gcm: GCMResult?): String = buildString {
    val printed = mutableSetOf<Node>()

    fun node(n: Node, forcedColor: String? = null): String {
        val color = when {
            forcedColor != null -> forcedColor
            n is Projection -> projectionColor
            n is ControlFlow -> controlColor
            else -> valueColor
        }
        val shape = when (n) {
            is BlockExit -> blockExitShape
            is ControlFlow -> controlShape
            else -> commonShape
        }
        printed += n
        return "${n.id}[label=\"${n}\",color=\"$color\",shape=$shape];"
    }

    fun arg(arg: Node, argNum: Int, node: Node): String {
        val name = node.paramName(argNum)
        val color = when {
            node is BlockEntry -> controlColor
            node is Controlled && argNum == 0 -> controlColor
            node is IfProjection && argNum == 0 -> controlColor
            node is Unwind && argNum == 0 -> controlColor
            //node is Projection -> projectionColor
            //node is Phi && argNum == 0 -> controlColor // FIXME
            else -> valueColor
        }
        val extraAttrs = when {
            //node is Unwind && argNum == 0 -> listOf("constraint" to "false")
            else -> emptyList<Pair<String, String>>()
        }
        val attrs = listOf(
            "color" to color,
            "label" to name,
            "fontcolor" to color,
            "dir" to "back"
        ) + extraAttrs
        return "${arg.id}->${node.id}[${attrs.joinToString(",") { (k, v) -> "$k=\"$v\"" }}]"
    }

    fun block(gcm: GCMResult, b: BlockEntry) {
        appendLine("subgraph cluster_${b.id} {")
        appendLine("style=filled; color=\"$clusterColor\";")
        //appendLine("label=\"Block ${b.id}\"")

        // TODO propper getter
        val blockNodes = gcm.linearOrder(b)
        for (n in blockNodes) {
            appendLine(node(n))
        }
        appendLine("}")
    }

    appendLine("digraph Nodes {")
    gcm?.let {
        for (b in dfs(this@generateGraphviz.cfg())) {
            block(gcm, b)
        }
    }

    for (n in allNodes()) {
        if (n !in printed) {
            appendLine(node(n, unorderedColor))
        }
        for ((idx, arg) in n.args.filterNotNull().withIndex()) {
            appendLine(arg(arg, idx, n))
        }
    }

    appendLine("}")
}

context(gcm: GCMResult)
fun Session.generateGraphviz() = generateGraphviz(gcm)

fun Session.generateGraphviz() = generateGraphviz(null)

fun Session.printGraphviz() {
    println(withGCM { generateGraphviz() })
}

fun Session.printGraphvizNoGCM() {
    println(generateGraphviz())
}
