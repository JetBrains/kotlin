/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.parser.swc

import com.caoccao.javet.swc4j.Swc4j
import com.caoccao.javet.swc4j.ast.interfaces.ISwc4jAst
import com.caoccao.javet.swc4j.enums.Swc4jMediaType
import com.caoccao.javet.swc4j.enums.Swc4jParseMode
import com.caoccao.javet.swc4j.options.Swc4jParseOptions
import com.caoccao.javet.swc4j.outputs.Swc4jParseOutput
import org.jetbrains.kotlin.js.backend.ast.JsNode
import java.net.URL

object SwcBasedJsParser {
    fun parseProgram(code: String, codeOffset: Int, codePosition: CodePosition): JsNode? {
        val output = parse(code, codeOffset, codePosition)
        return SwcJsAstMapper.map(output.program.body)
    }

    private fun parse(code: String, codeOffset: Int, codePosition: CodePosition): Swc4jParseOutput {
        val swc = Swc4j()
        val options = Swc4jParseOptions()
            .apply {
                specifier = URL("file:///Validator.ts")
                mediaType = Swc4jMediaType.JavaScript
                isCaptureAst = true
                parseMode = Swc4jParseMode.Script
            }
        return swc.parse(code, options)
    }
}

object SwcJsAstMapper {
    fun map(nodes: List<ISwc4jAst>): List<JsNode>? {

    }

    fun map(node: ISwc4jAst): JsNode? {

    }
}

data class CodePosition(val line: Int, val column: Int)