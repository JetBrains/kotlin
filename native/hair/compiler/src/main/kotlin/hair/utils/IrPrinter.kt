package hair.utils

import hair.graph.dfs
import hair.ir.*
import hair.ir.nodes.*
import hair.transform.*
import hair.utils.IrPrinter.clusterColor
import hair.utils.IrPrinter.commonShape
import hair.utils.IrPrinter.controlColor
import hair.utils.IrPrinter.controlShape
import hair.utils.IrPrinter.projectionColor
import hair.utils.IrPrinter.projectionShape
import hair.utils.IrPrinter.valueColor

object IrPrinter {
    const val controlColor = "black"
    const val valueColor = "#FE2857"
    const val clusterColor = "#CDCDCD"
    const val projectionColor = "#087CFA"

    const val controlShape = "rectangle"
    const val commonShape = "ellipse"
    const val projectionShape = "diamond"
}

fun Session.printGraphviz() {
    fun graphvizArg(arg: Node, argNum: Int, node: Node): String {
        val color = if (node is Projection) projectionColor else valueColor
        val name = node.paramName(argNum)
        return "${arg.id}->${node.id}[color=\"$color\", label=\"$name\", fontcolor=\"$color\", dir=back]"
    }

    fun printControls(node: ControlFlow) {
        val inputs: List<Pair<Node, String>> = when (node) {
            is Controlled -> listOfNotNull(node.prevControl?.let { it to "" })
            is Block -> node.enters.map {
                when (it) {
                    is SingleExit -> it to ""
                    is TwoExits -> {
                        require(it.trueExit != it.falseExit) // TODO move somewhere
                        val label = if (it.trueExit == node) "True" else "False"
                        it to label
                    }
                    else -> shouldNotReachHere(it)
                }
            }
            else -> emptyList()
        }
        for ((input, label) in inputs) {
            println("${input.id}->${node.id}[color=\"$controlColor\", label=\"$label\"]")
        }
        val exceptionalInputs: List<Throwing> = when (node) {
            is Catching -> node.throwers
            else -> emptyList()
        }
        for (thrower in exceptionalInputs) {
            println("${thrower.id}->${node.id}[color=\"$controlColor\", style=\"dashed\"]")
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
            is Projection -> projectionShape
            is ControlFlow -> controlShape
            else -> commonShape
        }
        printed += n
        return "${n.id}[label=\"${n}\",color=\"$color\",shape=$shape];"
    }

//    fun printBlock(gcm: GCMResult, b: Block) {
//        println("subgraph cluster_${b.id} {")
//        println("style=filled; color=\"$clusterColor\";")
//        // TODO cluster label?
//
//        // TODO propper getter
//        val blockNodes = gcm.linearOrder(b)
//        for (n in blockNodes) {
//            println(graphvizNode(n))
//        }
//        println("}")
//        for (n in blockNodes) {
//            if (n is ControlFlow) {
//                printControls(n)
//            }
//        }
//    }
    fun printBlock(gcm: GCMResult, b: ControlFlow) {
        println("subgraph cluster_${b.id} {")
        println("style=filled; color=\"$clusterColor\";")
        // TODO cluster label?

        // TODO propper getter
        val blockNodes = gcm.linearOrder(b)
        for (n in blockNodes) {
            println(graphvizNode(n))
        }
        println("}")
        for (n in blockNodes) {
            if (n is ControlFlow) {
                printControls(n)
            }
        }
    }

    println("digraph Nodes {")
    println()
    withGCM { gcm ->
        for (b in dfs(this@printGraphviz.cfg())) {
            printBlock(gcm, b)
            println()
        }
    }
    for (n in allNodes()) {
        if (n !in printed) {
            println(graphvizNode(n, "blue"))
            if (n is ControlFlow) {
                printControls(n)
            }
        }
        for ((idx, arg) in n.args.filterNotNull().withIndex()) {
            println(graphvizArg(arg, idx, n))
        }
    }
    println("}")
}
