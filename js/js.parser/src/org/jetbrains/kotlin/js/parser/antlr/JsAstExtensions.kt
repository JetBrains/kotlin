/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.parser.antlr

import com.google.gwt.dev.js.rhino.CodePosition
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.jetbrains.kotlin.js.backend.ast.JsLocation

internal fun ParserRuleContext.getJsAstLocation(file: String): JsLocation {
    return JsLocation(file, start.line, start.charPositionInLine)
}

internal fun ParserRuleContext.getJsAstLocation(origin: JsLocation): JsLocation {
    return JsLocation(origin.file, origin.startLine + start.line, origin.startChar + start.charPositionInLine)
}

internal val ParserRuleContext.startPosition: CodePosition
    get() = start.codePosition

internal val ParserRuleContext.stopPosition: CodePosition
    get() = stop.codePosition

internal val Token.codePosition: CodePosition
    get() = CodePosition(line, charPositionInLine)