package hair.utils

import hair.graph.dfs
import hair.ir.*
import hair.ir.nodes.*
import hair.transform.*
import hair.utils.IrPrinter.blockExitShape
import hair.utils.IrPrinter.clusterColor
import hair.utils.IrPrinter.commonShape
import hair.utils.IrPrinter.controlColor
import hair.utils.IrPrinter.controlShape
import hair.utils.IrPrinter.projectionColor
import hair.utils.IrPrinter.valueColor

object IrPrinter {
    const val controlColor = "black"
    const val valueColor = "#FE2857"
    const val clusterColor = "#CDCDCD"
    const val projectionColor = "#087CFA"

    const val controlShape = "rectangle"
    const val commonShape = "ellipse"
    const val blockExitShape = "diamond"
    //const val projectionShape = "diamond"
}

fun Session.printGraphviz() {
    fun graphvizArg(arg: Node, argNum: Int, node: Node): String {
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

    fun printControls(node: ControlFlow) {
        val inputs: List<Node> = when (node) {
            is Controlled -> listOf(node.control)
            is BlockEntry -> node.preds.toList()
            else -> emptyList()
        }
        for (input in inputs) {
            println("${node.id}->${input.id}[color=\"$controlColor\"]")
        }
        val exceptionalInputs: List<Throwing> = when (node) {
            is Unwind -> listOf(node.thrower)
            else -> emptyList()
        }
        for (thrower in exceptionalInputs) {
            println("${node.id}->${thrower.id}[color=\"$controlColor\", style=\"dashed\"]")
        }
    }

    val printed = mutableSetOf<Node>()

    fun graphvizNode(n: Node, forcedColor: String? = null): String {
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

    context(gcm: GCMResult)
    fun printBlock(b: BlockEntry) {
        println("subgraph cluster_${b.id} {")
        println("style=filled; color=\"$clusterColor\";")
        // TODO cluster label?

        // TODO propper getter
        val blockNodes = gcm.linearOrder(b)
        for (n in blockNodes) {
            println(graphvizNode(n))
        }
        println("}")
    }

    println("digraph Nodes {")
    // println("rankdir=\"BT\"")
    println()
    withGCM {
        for (b in dfs(this@printGraphviz.cfg())) {
            printBlock(b)
            println()
        }
    }
    for (n in allNodes()) {
        if (n !in printed) {
            println(graphvizNode(n, "blue"))
        }
        for ((idx, arg) in n.args.filterNotNull().withIndex()) {
            println(graphvizArg(arg, idx, n))
        }
    }
    println("}")
}
