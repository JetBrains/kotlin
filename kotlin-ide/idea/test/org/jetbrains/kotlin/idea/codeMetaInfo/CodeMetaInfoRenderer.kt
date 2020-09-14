/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeMetaInfo

import com.intellij.util.containers.Stack
import org.jetbrains.kotlin.checkers.utils.CheckerTestUtil
import org.jetbrains.kotlin.idea.codeMetaInfo.models.ICodeMetaInfo
import java.io.File

object CodeMetaInfoRenderer {

    fun renderTagsToText(
        codeMetaInfos: List<ICodeMetaInfo>,
        originalText: String
    ): StringBuffer {
        val result = StringBuffer()
        if (codeMetaInfos.isEmpty()) {
            result.append(originalText)
            return result
        }
        val sortedMetaInfos = getSortedCodeMetaInfos(codeMetaInfos)
        val opened = Stack<ICodeMetaInfo>()

        for (i in originalText.indices) {
            val c = originalText[i]
            var prev: ICodeMetaInfo? = null

            while (!opened.isEmpty() && i == opened.peek().end) {
                if (prev == null || prev.start != opened.peek().start)
                    closeString(result)
                prev = opened.pop()
            }
            if (sortedMetaInfos.any { it.start == i }) {
                openStartTag(result)
                val matchedCodeMetaInfos = sortedMetaInfos.filter { it.start == i }.toMutableList()
                val iterator = matchedCodeMetaInfos.listIterator()
                var current: ICodeMetaInfo? = iterator.next()

                while (current != null) {
                    val next: ICodeMetaInfo? = if (iterator.hasNext()) iterator.next() else null
                    opened.push(current)
                    result.append(current.asString())
                    when {
                        next == null ->
                            closeStartTag(result)
                        next.end == current.end ->
                            result.append(", ")
                        else ->
                            closeStartAndOpenNewTag(result)
                    }
                    current = next
                }
            }
            result.append(c)
        }
        var prev: ICodeMetaInfo? = null

        while (!opened.isEmpty() && originalText.length == opened.peek().end) {
            if (prev == null || prev.start != opened.peek().start)
                closeString(result)
            prev = opened.pop()
        }
        return result
    }

    private fun getSortedCodeMetaInfos(
        metaInfos: Collection<ICodeMetaInfo>,
    ): MutableList<ICodeMetaInfo> {
        val result = metaInfos.toMutableList()
        result.sortWith(Comparator { d1: ICodeMetaInfo, d2: ICodeMetaInfo ->
            if (d1.start != d2.start) d1.start - d2.start else d2.end - d1.end
        })
        return result
    }

    private fun closeString(result: StringBuffer) = result.append("<!>")
    private fun openStartTag(result: StringBuffer) = result.append("<!")
    private fun closeStartTag(result: StringBuffer) = result.append("!>")
    private fun closeStartAndOpenNewTag(result: StringBuffer) = result.append("!><!")
}

fun clearFileFromDiagnosticMarkup(file: File) {
    val text = file.readText()
    val cleanText = clearTextFromDiagnosticMarkup(text)
    file.writeText(cleanText)
}

fun clearTextFromDiagnosticMarkup(text: String): String = CheckerTestUtil.rangeStartOrEndPattern.matcher(text).replaceAll("")