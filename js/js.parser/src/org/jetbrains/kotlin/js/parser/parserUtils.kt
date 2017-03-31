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

package org.jetbrains.kotlin.js.parser

import com.google.gwt.dev.js.*
import com.google.gwt.dev.js.rhino.*

import org.jetbrains.kotlin.js.backend.ast.JsFunction
import org.jetbrains.kotlin.js.backend.ast.JsFunctionScope
import org.jetbrains.kotlin.js.backend.ast.JsScope
import org.jetbrains.kotlin.js.backend.ast.JsStatement
import org.jetbrains.kotlin.js.common.SourceInfoImpl
import java.io.*
import java.util.*

private val FAKE_SOURCE_INFO = SourceInfoImpl(null, 0, 0, 0, 0)

fun parse(code: String, reporter: ErrorReporter, scope: JsScope, fileName: String): List<JsStatement> {
        val insideFunction = scope is JsFunctionScope
        val node = parse(code, 0, reporter, insideFunction, Parser::parse)
        return node.toJsAst(scope, fileName) {
            mapStatements(it)
        }
}

fun parseFunction(code: String, fileName: String, offset: Int, reporter: ErrorReporter, scope: JsScope): JsFunction =
        parse(code, offset, reporter, insideFunction = false) {
            addObserver(FunctionParsingObserver())
            primaryExpr(it)
        }.toJsAst(scope, fileName, JsAstMapper::mapFunction)

private class FunctionParsingObserver : Observer {
    var functionsStarted = 0

    override fun update(o: Observable?, arg: Any?) {
        when (arg) {
            is ParserEvents.OnFunctionParsingStart -> {
                functionsStarted++
            }
            is ParserEvents.OnFunctionParsingEnd -> {
                functionsStarted--

                if (functionsStarted == 0) {
                    arg.tokenStream.ungetToken(TokenStream.EOF)
                }
            }
        }
    }
}

inline
private fun parse(
        code: String,
        offset: Int,
        reporter: ErrorReporter,
        insideFunction: Boolean,
        parseAction: Parser.(TokenStream)->Any
): Node {
    Context.enter().errorReporter = reporter

    try {
        val ts = TokenStream(StringReader(code, offset), "<parser>", FAKE_SOURCE_INFO.line)
        val parser = Parser(IRFactory(ts), insideFunction)
        return parser.parseAction(ts) as Node
    } finally {
        Context.exit()
    }
}

inline
private fun <T> Node.toJsAst(scope: JsScope, fileName: String, mapAction: JsAstMapper.(Node)->T): T =
        JsAstMapper(scope, fileName).mapAction(this)

private fun StringReader(string: String, offset: Int): Reader {
    val reader = StringReader(string)
    reader.skip(offset.toLong())
    return reader
}