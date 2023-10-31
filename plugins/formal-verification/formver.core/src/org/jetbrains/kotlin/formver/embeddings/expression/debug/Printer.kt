/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.expression.debug

import kotlin.math.max

/**
 * Helper class for pretty-printing tree views.
 *
 * Provides support for automatic handling of indentation and tracks line length for the user.
 */
class Printer(
    /**
     * Number of spaces to add per indent step.
     */
    private val indentStep: Int = 4,
    /**
     * Maximum line length we recommend that clients use.
     *
     * This is not enforced, but clients who want to produce pretty output can use this to make decisions on where to break
     * up their output.
     */
    private val desiredMaxLineLength: Int = 120,
) {
    // _currentLine is non-null precisely when a line is currently being constructed.
    private var _currentLine: StringBuilder? = null

    // currentLine starts on a line if one isn't already being constructed.
    private val currentLine: StringBuilder
        get() {
            if (_currentLine == null) {
                _currentLine = StringBuilder(" ".repeat(indentLevel))
            }
            return _currentLine!!
        }
    private val lines = mutableListOf<String>()
    private var indentLevel = 0

    fun print(ast: TreeView) {
        with(ast) {
            this@Printer.printImpl()
        }
    }

    fun print(text: String) {
        currentLine.append(text)
    }

    fun newLine() {
        lines.add(currentLine.toString())
        _currentLine = null
    }

    fun indent() {
        assert(_currentLine == null) { "Indenting in the middle of a line has unclear semantics; don't do it." }
        indentLevel += indentStep
    }

    fun unindent() {
        assert(_currentLine == null) { "Unindenting in the middle of a line has unclear semantics; don't do it." }
        assert(indentLevel >= indentStep) { "Cannot unindent below zero." }
        indentLevel -= indentStep
    }

    /**
     * The number of characters remaining until the desired max line length is reached. Equals zero if all space is used up.
     */
    val availableSpace: Int
        get() = max(0, desiredMaxLineLength - currentLine.length)

    fun getResult(): String {
        // Ensure we got all the partial results.
        if (_currentLine != null) newLine()

        return lines.joinToString("\n").also { lines.clear() }
    }
}