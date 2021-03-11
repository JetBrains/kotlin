/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scratch.output

import com.intellij.diff.util.DiffUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.executeCommand
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.FoldRegion
import com.intellij.openapi.editor.FoldingModel
import com.intellij.openapi.editor.markup.*
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.idea.scratch.ScratchExpression
import org.jetbrains.kotlin.idea.scratch.ScratchFile
import java.util.*
import kotlin.math.max

/**
 * Output handler to print scratch output to separate window using [previewOutputBlocksManager].
 *
 * Multiline outputs from single expressions are folded.
 */
class PreviewEditorScratchOutputHandler(
    private val previewOutputBlocksManager: PreviewOutputBlocksManager,
    private val toolwindowHandler: ScratchOutputHandler,
    private val parentDisposable: Disposable
) : ScratchOutputHandler {

    override fun onStart(file: ScratchFile) {
        toolwindowHandler.onStart(file)
    }

    override fun handle(file: ScratchFile, expression: ScratchExpression, output: ScratchOutput) {
        printToPreviewEditor(expression, output)
    }

    override fun error(file: ScratchFile, message: String) {
        toolwindowHandler.error(file, message)
    }

    override fun onFinish(file: ScratchFile) {
        toolwindowHandler.onFinish(file)
    }

    override fun clear(file: ScratchFile) {
        toolwindowHandler.clear(file)

        clearOutputManager()
    }

    private fun printToPreviewEditor(expression: ScratchExpression, output: ScratchOutput) {
        TransactionGuard.submitTransaction(parentDisposable, Runnable {
            val targetCell = previewOutputBlocksManager.getBlock(expression) ?: previewOutputBlocksManager.addBlockToTheEnd(expression)
            targetCell.addOutput(output)
        })
    }

    private fun clearOutputManager() {
        TransactionGuard.submitTransaction(parentDisposable, Runnable {
            previewOutputBlocksManager.clear()
        })
    }
}

private val ScratchExpression.height: Int get() = lineEnd - lineStart + 1

interface ScratchOutputBlock {
    val sourceExpression: ScratchExpression
    val lineStart: Int
    val lineEnd: Int
    fun addOutput(output: ScratchOutput)
}

class PreviewOutputBlocksManager(editor: Editor) {
    private val targetDocument: Document = editor.document
    private val foldingModel: FoldingModel = editor.foldingModel
    private val markupModel: MarkupModel = editor.markupModel

    private val blocks: NavigableMap<ScratchExpression, OutputBlock> = TreeMap(Comparator.comparingInt { it.lineStart })

    fun computeSourceToPreviewAlignments(): List<Pair<Int, Int>> = blocks.values.map { it.sourceExpression.lineStart to it.lineStart }

    fun getBlock(expression: ScratchExpression): ScratchOutputBlock? = blocks[expression]

    fun addBlockToTheEnd(expression: ScratchExpression): ScratchOutputBlock = OutputBlock(expression).also {
        if (blocks.putIfAbsent(expression, it) != null) {
            error("There is already a cell for $expression!")
        }
    }

    fun clear() {
        blocks.clear()
        runWriteAction {
            executeCommand {
                targetDocument.setText("")
            }
        }
    }

    private inner class OutputBlock(override val sourceExpression: ScratchExpression) : ScratchOutputBlock {
        private val outputs: MutableList<ScratchOutput> = mutableListOf()

        override var lineStart: Int = computeCellLineStart(sourceExpression)
            private set

        override val lineEnd: Int get() = lineStart + countNewLines(outputs)

        val height: Int get() = lineEnd - lineStart + 1
        private var foldRegion: FoldRegion? = null

        override fun addOutput(output: ScratchOutput) {
            printAndSaveOutput(output)

            blocks.lowerEntry(sourceExpression)?.value?.updateFolding()
            blocks.tailMap(sourceExpression).values.forEach {
                it.recalculatePosition()
                it.updateFolding()
            }
        }

        /**
         * We want to make sure that changes in document happen in single edit, because if they are not,
         * listeners may see inconsistent document, which may cause troubles if they will try to highlight it
         * in some way. That's why it is important that [insertStringAtLine] does only one insert in the document,
         * and [output] is inserted into the [outputs] before the edits, so [OutputBlock] can correctly see
         * all its output expressions and highlight the whole block.
         */
        private fun printAndSaveOutput(output: ScratchOutput) {
            val beforeAdding = lineEnd
            val currentOutputStartLine = if (outputs.isEmpty()) lineStart else beforeAdding + 1

            outputs.add(output)

            runWriteAction {
                executeCommand {
                    targetDocument.insertStringAtLine(currentOutputStartLine, output.text)
                }
            }

            markupModel.highlightLines(currentOutputStartLine, lineEnd, getAttributesForOutputType(output.type))
        }

        private fun recalculatePosition() {
            lineStart = computeCellLineStart(sourceExpression)
        }

        private fun updateFolding() {
            foldingModel.runBatchFoldingOperation {
                foldRegion?.let(foldingModel::removeFoldRegion)

                if (height <= sourceExpression.height) return@runBatchFoldingOperation

                val firstFoldedLine = lineStart + (sourceExpression.height - 1)
                val placeholderLine = "${targetDocument.getLineContent(firstFoldedLine)}..."

                foldRegion = foldingModel.addFoldRegion(
                    targetDocument.getLineStartOffset(firstFoldedLine),
                    targetDocument.getLineEndOffset(lineEnd),
                    placeholderLine
                )

                foldRegion?.isExpanded = isLastCell && isOutputSmall
            }
        }

        private val isLastCell: Boolean get() = false // blocks.higherEntry(sourceExpression) == null
        private val isOutputSmall: Boolean get() = true
    }

    private fun computeCellLineStart(scratchExpression: ScratchExpression): Int {
        val previous = blocks.lowerEntry(scratchExpression)?.value ?: return scratchExpression.lineStart

        val distanceBetweenSources = scratchExpression.lineStart - previous.sourceExpression.lineEnd
        val differenceBetweenSourceAndOutputHeight = previous.sourceExpression.height - previous.height
        val compensation = max(differenceBetweenSourceAndOutputHeight, 0)
        return previous.lineEnd + compensation + distanceBetweenSources
    }

    fun getBlockAtLine(line: Int): ScratchOutputBlock? = blocks.values.find { line in it.lineStart..it.lineEnd }
}

private fun countNewLines(list: List<ScratchOutput>) = list.sumBy { StringUtil.countNewLines(it.text) } + max(list.size - 1, 0)

private fun Document.getLineContent(lineNumber: Int) =
    DiffUtil.getLinesContent(this, lineNumber, lineNumber + 1).toString()

private fun Document.insertStringAtLine(lineNumber: Int, text: String) {
    val missingNewLines = lineNumber - (DiffUtil.getLineCount(this) - 1)
    if (missingNewLines > 0) {
        insertString(textLength, "${"\n".repeat(missingNewLines)}$text")
    } else {
        insertString(getLineStartOffset(lineNumber), text)
    }
}

fun MarkupModel.highlightLines(
    from: Int,
    to: Int,
    attributes: TextAttributes,
    targetArea: HighlighterTargetArea = HighlighterTargetArea.EXACT_RANGE
): RangeHighlighter {
    val fromOffset = document.getLineStartOffset(from)
    val toOffset = document.getLineEndOffset(to)

    return addRangeHighlighter(
        fromOffset,
        toOffset,
        HighlighterLayer.CARET_ROW,
        attributes,
        targetArea
    )
}
