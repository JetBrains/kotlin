/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import org.jetbrains.kotlin.renderer.Renderer
import org.jetbrains.kotlin.js.resolve.diagnostics.JsCallData
import org.jetbrains.kotlin.js.resolve.diagnostics.JsCallDataWithCode
import com.google.gwt.dev.js.rhino.Utils.isEndOfLine

abstract class JsCallDataRenderer : Renderer<JsCallData> {
    protected abstract fun format(data: JsCallDataWithCode): String

    override fun render(data: JsCallData?): String =
            when (data) {
                is JsCallDataWithCode -> format(data)
                is JsCallData -> data.message
                else -> throw AssertionError("Cannot render null data")
            }
}

object JsCallDataTextRenderer : JsCallDataRenderer() {
    override fun format(data: JsCallDataWithCode): String {
        val codeRange = data.codeRange
        val code = data.code.underlineAsText(codeRange.getStartOffset(), codeRange.getEndOffset())
        return "${data.message} in code:\n${code}"
    }
}

public object JsCallDataHtmlRenderer : JsCallDataRenderer() {
    override fun format(data: JsCallDataWithCode): String {
        val codeRange = data.codeRange
        val code = data.code.underlineAsHtml(codeRange.getStartOffset(), codeRange.getEndOffset())
        return "${data.message} in code:<br><pre>${code}</pre>"
    }
}

/**
 * Underlines string in given rage.
 *
 * For example:
 * var  = 10;
 *    ^^^^
 */
public fun String.underlineAsText(from: Int, to: Int): String {
    val lines = StringBuilder()
    var marks = StringBuilder()
    var lineWasMarked = false

    for (i in indices) {
        val c = charAt(i)
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
                lines.appendln(marks.toString().trimTrailing())
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

public fun String.underlineAsHtml(from: Int, to: Int): String {
    val lines = StringBuilder()
    var openMarker = false
    val underlineStart = "<u>"
    val underlineEnd = "</u>"

    for (i in indices) {
        val c = charAt(i)

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