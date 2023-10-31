/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.expression.debug

interface TreeView {
    // Width of the node if we assume it's all laid out on one line.
    val widthGuess: Int

    fun Printer.printImpl()
}

fun TreeView.print(): String {
    val printer = Printer()
    printer.print(this)
    return printer.getResult()
}

class NamedBranchingNode(val name: String, val args: List<TreeView>) : TreeView {
    constructor(name: String, vararg args: TreeView) : this(name, args.toList())

    // Characters for parentheses
    private val fixedExtraWidth = 2

    // Characters for comma and space
    private val perArgumentExtraWidth = 2

    // We overcount by 2 since we assume there's a comma and space after the last character as well; it's probably not
    // worth fixing at this point.
    override val widthGuess: Int = name.length + fixedExtraWidth + args.map { it.widthGuess + perArgumentExtraWidth }.sum()

    override fun Printer.printImpl() {
        val splitOverLines = availableSpace <= widthGuess
        print(name)
        print("(")
        if (splitOverLines) {
            newLine()
            indent()
            for (arg in args) {
                print(arg)
                print(",")
                newLine()
            }
            unindent()
        } else if (args.isNotEmpty()) {
            for (arg in args.take(args.size - 1)) {
                print(arg)
                print(", ")
            }
            print(args.last())
        } // No else; if the args are empty, we don't need to do anything.
        print(")")
    }
}

class PlaintextLeaf(val text: String) : TreeView {
    override val widthGuess: Int
        get() = text.length

    override fun Printer.printImpl() {
        print(text)
    }
}

/**
 * Node with a label that refers to its purpose.
 *
 * `LabeledNode` would also be an appropriate name, but we already use the term "label" frequently.
 */
class DesignatedNode(val label: String, val node: TreeView) : TreeView {
    // Characters for equals sign separator.
    private val fixedExtraWidth = 3

    override val widthGuess: Int = label.length + fixedExtraWidth + node.widthGuess

    override fun Printer.printImpl() {
        print(label)
        print(" = ")
        print(node)
    }
}