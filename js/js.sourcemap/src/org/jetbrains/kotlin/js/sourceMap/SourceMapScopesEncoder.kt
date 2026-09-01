/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.sourceMap

import org.jetbrains.kotlin.js.backend.ast.metadata.JsOriginalScopeNode

class SourceMapScopesEncoder(private val sourceMapBuilder: SourceMap3Builder) {
    fun encodeOriginalScopes(fileScopeTrees: List<JsOriginalScopeNode>): List<String> =
        fileScopeTrees.map { encodeOriginalScope(it) }

    fun encodeOriginalScope(fileTree: JsOriginalScopeNode): String {
        val buffer = StringBuilder()
        val cursor = RelativeValuesCursor()
        context(buffer, cursor) {
            encodeOriginalScopeTree(fileTree)
        }

        return buffer.toString()
    }

    context(buffer: StringBuilder, cursor: RelativeValuesCursor)
    private fun encodeOriginalScopeTree(node: JsOriginalScopeNode) {
        encodeOriginalScopeStart(node)

        if (node.variables.isNotEmpty()) {
            buffer.append(',')
            encodeOriginalScopeVariables(node)
        }

        if (node.children.isNotEmpty()) {
            encodeOriginalScopeTreeList(node.children)
        }

        buffer.append(',')

        encodeOriginalScopeEnd(node)
    }

    context(buffer: StringBuilder, cursor: RelativeValuesCursor)
    private fun encodeOriginalScopeStart(node: JsOriginalScopeNode) {
        buffer.append('B')

        var flags = 0u
        if (node.name != null) flags = flags or 0x1u
        if (node.kind != null) flags = flags or 0x2u
        if (node.isStackFrame) flags = flags or 0x4u

        Base64VLQ.encodeUnsigned(buffer, flags.toInt())

        cursor.updatePosition(node.startLine, node.startColumn) { lineDelta, columnDelta ->
            Base64VLQ.encodeUnsigned(buffer, lineDelta)
            Base64VLQ.encodeUnsigned(buffer, columnDelta)
        }

        // ScopeNameOrKind slot: name if present, else kind (mirrors originalScopeName/-Kind read order)
        node.name?.let {
            cursor.updateNameIndex(sourceMapBuilder.getNameIndex(it)) { nameDelta ->
                Base64VLQ.encode(buffer, nameDelta)
            }
        } ?: node.kind?.let {
            cursor.updateKindIndex(sourceMapBuilder.getNameIndex(it)) { kindDelta ->
                Base64VLQ.encode(buffer, kindDelta)
            }
        }

        // ScopeKind slot: only emitted when BOTH name and kind are present
        if (node.name != null && node.kind != null) {
            cursor.updateKindIndex(sourceMapBuilder.getNameIndex(node.kind!!)) { kindDelta ->
                Base64VLQ.encode(buffer, kindDelta)
            }
        }
    }

    context(buffer: StringBuilder, cursor: RelativeValuesCursor)
    private fun encodeOriginalScopeVariables(node: JsOriginalScopeNode) {
        buffer.append('D')
        encodeScopeVariablesList(node)
    }

    context(buffer: StringBuilder, cursor: RelativeValuesCursor)
    private fun encodeScopeVariablesList(node: JsOriginalScopeNode) {
        for (variable in node.variables) {
            cursor.updateVariableIndex(sourceMapBuilder.getNameIndex(variable)) { variableDelta ->
                Base64VLQ.encode(buffer, variableDelta)
            }
        }
    }

    context(buffer: StringBuilder, cursor: RelativeValuesCursor)
    private fun encodeOriginalScopeTreeList(nodes: List<JsOriginalScopeNode>) {
        for (child in nodes) {
            buffer.append(',')
            encodeOriginalScopeTree(child)
        }
    }

    context(buffer: StringBuilder, cursor: RelativeValuesCursor)
    private fun encodeOriginalScopeEnd(node: JsOriginalScopeNode) {
        buffer.append('C')
        cursor.updatePosition(node.endLine, node.endColumn) { lineDelta, columnDelta ->
            Base64VLQ.encodeUnsigned(buffer, lineDelta)
            Base64VLQ.encodeUnsigned(buffer, columnDelta)
        }
    }

    private data class RelativeValuesCursor(
        var nameIndex: Int = 0,
        var kindIndex: Int = 0,
        var variableIndex: Int = 0,
        var absoluteLine: Int = 0,
        var absoluteColumn: Int = 0
    ) {
        fun updateNameIndex(newNameIndex: Int, deltaApplier: (Int) -> Unit) {
            deltaApplier(newNameIndex - nameIndex)
            nameIndex = newNameIndex
        }

        fun updateKindIndex(newKindIndex: Int, deltaApplier: (Int) -> Unit) {
            deltaApplier(newKindIndex - kindIndex)
            kindIndex = newKindIndex
        }

        fun updateVariableIndex(newVariableIndex: Int, deltaApplier: (Int) -> Unit) {
            deltaApplier(newVariableIndex - variableIndex)
            variableIndex = newVariableIndex
        }

        fun updatePosition(newLine: Int, newColumn: Int, deltaApplier: (Int, Int) -> Unit) {
            val lineDelta = newLine - absoluteLine
            val columnDelta = if (lineDelta == 0) newColumn - absoluteColumn else newColumn
            deltaApplier(lineDelta, columnDelta)
            absoluteLine = newLine
            absoluteColumn = newColumn
        }
    }
}
