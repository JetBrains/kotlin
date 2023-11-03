/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.expression.debug

import org.jetbrains.kotlin.formver.forEachWithIsLast
import org.jetbrains.kotlin.formver.mapWithIsLast

interface TreeView {
    // Width of the node if we assume it's all laid out on one line.
    val widthGuess: Int

    fun Printer.printImpl()

    val Printer.fitsOnSingleLine: Boolean
        get() = widthGuess <= availableSpace
}

fun TreeView.print(): String {
    val printer = Printer()
    printer.print(this)
    return printer.getResult()
}

class NamedBranchingNode(val name: String, val args: List<TreeView>) : TreeView {
    constructor(name: String, vararg args: TreeView) : this(name, args.toList())

    companion object {
        private val scheme = SeparationSchemeImpl("(", ", ", ")")
    }

    override val widthGuess: Int = name.length + scheme.guessWidth(args)

    override fun Printer.printImpl() {
        print(name)
        printList(args, scheme, fitsOnSingleLine)
    }
}

class BlockNode(args: List<TreeView>) : TreeView {
    val flatArgs: List<TreeView> = args.flatMap {
        when (it) {
            is BlockNode -> it.flatArgs
            else -> listOf(it)
        }
    }

    companion object {
        private val scheme = SeparationSchemeImpl("{ ", "; ", " }")
    }

    override val widthGuess: Int
        get() = scheme.guessWidth(flatArgs)

    override fun Printer.printImpl() {
        printList(flatArgs, scheme, fitsOnSingleLine)
    }
}

class PlaintextLeaf(val text: String) : TreeView {
    override val widthGuess: Int
        get() = text.length

    override fun Printer.printImpl() {
        print(text)
    }
}

class OperatorNode(val left: TreeView, val op: String, val right: TreeView) : TreeView {
    override val widthGuess: Int = left.widthGuess + right.widthGuess + op.length

    override fun Printer.printImpl() {
        print(left)
        print(op)
        print(right)
    }
}

/**
 * Node with a label that refers to its purpose.
 *
 * `LabeledNode` would also be an appropriate name, but we already use the term "label" frequently.
 */
fun designatedNode(label: String, node: TreeView) = OperatorNode(PlaintextLeaf(label), " = ", node)

interface SeparationScheme {
    val begin: String
    val separator: String
    val end: String

    fun trim(): SeparationScheme
}

private class SeparationSchemeImpl(override val begin: String, override val separator: String, override val end: String) :
    SeparationScheme {
    val trimmed = object : SeparationScheme {
        override val begin: String = this@SeparationSchemeImpl.begin.trim()
        override val separator: String = this@SeparationSchemeImpl.separator.trim()
        override val end: String = this@SeparationSchemeImpl.end.trim()

        override fun trim(): SeparationScheme = this
    }

    override fun trim() = trimmed

    fun guessWidth(args: List<TreeView>): Int =
        if (args.isEmpty()) trimmed.begin.length + trimmed.end.length
        else begin.length + end.length + args.mapWithIsLast { arg, isLast -> arg.widthGuess + if (!isLast) separator.length else 0 }.sum()
}

private fun Printer.printList(args: List<TreeView>, scheme: SeparationScheme, singleLine: Boolean) {
    if (singleLine) singleLinePrintList(args, scheme)
    else multiLinePrintList(args, scheme.trim())
}

private fun Printer.singleLinePrintList(args: List<TreeView>, scheme: SeparationScheme) {
    print(scheme.begin)
    args.forEachWithIsLast { arg, isLast ->
        print(arg)
        if (!isLast) print(scheme.separator)
    }
    print(scheme.end)
}

private fun Printer.multiLinePrintList(args: List<TreeView>, scheme: SeparationScheme) {
    print(scheme.begin)
    newLine()
    indent()
    for (arg in args) {
        print(arg)
        print(scheme.separator)
        newLine()
    }
    unindent()
    print(scheme.end)
}