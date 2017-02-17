/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.js.resolve.diagnostics

import com.google.gwt.dev.js.rhino.Utils.isEndOfLine
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticParameterRenderer
import org.jetbrains.kotlin.diagnostics.rendering.RenderingContext

object RenderFirstLineOfElementText : DiagnosticParameterRenderer<PsiElement> {
    override fun render(element: PsiElement, context: RenderingContext): String {
        val text = element.text
        val index = text.indexOf('\n')
        return if (index == -1) text else text.substring(0, index) + "..."
    }
}

abstract class JsCallDataRenderer : DiagnosticParameterRenderer<JsCallData> {
    protected abstract fun format(data: JsCallDataWithCode): String

    override fun render(data: JsCallData, context: RenderingContext): String =
            when (data) {
                is JsCallDataWithCode -> format(data)
                is JsCallData -> data.message
                else -> throw AssertionError("Cannot render null data")
            }
}

object JsCallDataTextRenderer : JsCallDataRenderer() {
    override fun format(data: JsCallDataWithCode): String {
        val codeRange = data.codeRange
        val code = data.code.underlineAsText(codeRange.startOffset, codeRange.endOffset)
        return "${data.message} in code:\n$code"
    }
}

object JsCallDataHtmlRenderer : JsCallDataRenderer() {
    override fun format(data: JsCallDataWithCode): String {
        val codeRange = data.codeRange
        val code = data.code.underlineAsHtml(codeRange.startOffset, codeRange.endOffset)
        return "${data.message} in code:<br><pre>$code</pre>"
    }
}

/**
 * Underlines string in given rage.
 *
 * For example:
 * var  = 10;
 *    ^^^^
 */
fun String.underlineAsText(from: Int, to: Int): String {
    val lines = StringBuilder()
    var marks = StringBuilder()
    var lineWasMarked = false

    for (i in indices) {
        val c = this[i]
        val mark: Char

        mark = when (i) {
            in from..to -> '^'
            else -> ' '
        }

        lines.append(c)
        marks.append(mark)
        lineWasMarked = lineWasMarked || mark != ' '

        if (isEndOfLine(c.toInt())) {
            if (lineWasMarked) {
                lines.appendln(marks.toString().trimEnd())
                lineWasMarked = false
            }

            marks = StringBuilder()
        }
    }

    if (lineWasMarked) {
        lines.appendln()
        lines.append(marks.toString())
    }

    return lines.toString()
}

fun String.underlineAsHtml(from: Int, to: Int): String {
    val lines = StringBuilder()
    var openMarker = false
    val underlineStart = "<u>"
    val underlineEnd = "</u>"

    for (i in indices) {
        val c = this[i]

        val mark = when (i) {
            from -> {
                openMarker = true
                underlineStart
            }
            to -> {
                openMarker = false
                underlineEnd
            }
            else -> ""
        }

        lines.append(mark)

        if (isEndOfLine(c.toInt()) && openMarker) {
            lines.append(underlineEnd + c + underlineStart)
        } else {
            lines.append(c)
        }
    }

    return lines.toString()
}