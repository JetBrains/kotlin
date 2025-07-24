/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.parser.antlr

import JavaScriptParser
import com.google.gwt.dev.js.parserExceptions.JsParserException
import org.jetbrains.kotlin.js.backend.ast.JsStatement

class JsAstMapper {
    @Throws(JsParserException::class)
    fun mapStatements(nodeStmts: JavaScriptParser.StatementListContext): MutableList<JsStatement?> {
        return nodeStmts.statement().map {
            mapStatement(it)
        }
    }

    private fun mapStatement(statement: JavaScriptParser.StatementContext): JsStatement? {

    }
}