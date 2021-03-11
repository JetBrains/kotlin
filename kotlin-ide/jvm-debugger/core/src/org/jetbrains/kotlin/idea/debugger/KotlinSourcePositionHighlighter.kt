/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger

import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.SourcePositionHighlighter
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.psi.KtFunctionLiteral

class KotlinSourcePositionHighlighter : SourcePositionHighlighter() {
    override fun getHighlightRange(sourcePosition: SourcePosition?): TextRange? {
        val lambda = sourcePosition?.elementAt?.parent
        if (lambda is KtFunctionLiteral) {
            return lambda.textRange
        }
        return null
    }
}
