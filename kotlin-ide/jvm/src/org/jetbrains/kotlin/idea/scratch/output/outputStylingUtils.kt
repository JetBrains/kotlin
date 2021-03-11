/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scratch.output

import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.ui.Colors
import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Font

fun getAttributesForOutputType(outputType: ScratchOutputType): TextAttributes {
    return when (outputType) {
        ScratchOutputType.OUTPUT -> userOutputAttributes
        ScratchOutputType.RESULT -> normalAttributes
        ScratchOutputType.ERROR -> errorAttributes
    }
}

private val userOutputColor = Color(0x5C5CFF)

private val normalAttributes = TextAttributes().apply {
    foregroundColor = JBColor.GRAY
    fontType = Font.ITALIC
}

private val errorAttributes = TextAttributes().apply {
    foregroundColor = JBColor(Colors.DARK_RED, Colors.DARK_RED)
    fontType = Font.ITALIC
}

private val userOutputAttributes = TextAttributes().apply {
    foregroundColor = JBColor(userOutputColor, userOutputColor)
    fontType = Font.ITALIC
}
