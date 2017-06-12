/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.dce

import com.google.gwt.dev.js.rhino.CodePosition
import com.google.gwt.dev.js.rhino.ErrorReporter
import org.jetbrains.kotlin.js.backend.ast.JsBlock
import org.jetbrains.kotlin.js.backend.ast.JsGlobalBlock
import org.jetbrains.kotlin.js.backend.ast.JsNode
import org.jetbrains.kotlin.js.backend.ast.JsProgram
import org.jetbrains.kotlin.js.dce.Context.Node
import org.jetbrains.kotlin.js.inline.util.collectDefinedNames
import org.jetbrains.kotlin.js.inline.util.fixForwardNameReferences
import org.jetbrains.kotlin.js.parser.parse
import java.io.File

class DeadCodeElimination(val logConsumer: (String) -> Unit) {
    val moduleMapping = mutableMapOf<JsBlock, String>()
    private val reachableNames = mutableSetOf<String>()

    var reachableNodes = setOf<Node>()
        get
        private set

    fun apply(root: JsNode) {
        val context = Context()

        val topLevelVars = collectDefinedNames(root)
        context.addNodesForLocalVars(topLevelVars)
        for (name in topLevelVars) {
            context.nodes[name]!!.alias(context.globalScope.member(name.ident))
        }

        val analyzer = Analyzer(context)
        analyzer.moduleMapping += moduleMapping
        root.accept(analyzer)

        val usageFinder = ReachabilityTracker(context, analyzer.analysisResult, logConsumer)
        root.accept(usageFinder)

        for (reachableName in reachableNames) {
            val path = reachableName.split(".")
            val node = path.fold(context.globalScope) { node, part -> node.member(part) }
            usageFinder.reach(node)
        }
        reachableNodes = usageFinder.reachableNodes

        Eliminator(analyzer.analysisResult).accept(root)
    }

    companion object {
        fun run(
                inputFiles: Collection<InputFile>,
                rootReachableNames: Set<String>,
                logConsumer: (String) -> Unit
        ): DeadCodeEliminationResult {
            val program = JsProgram()
            val dce = DeadCodeElimination(logConsumer)

            val blocks = inputFiles.map { file ->
                val block = JsGlobalBlock()
                val code = File(file.path).readText()
                block.statements += parse(code, reporter, program.scope, file.path)
                file.moduleName?.let { dce.moduleMapping[block] = it }
                block
            }
            program.globalBlock.statements += blocks
            program.globalBlock.fixForwardNameReferences()

            dce.reachableNames += rootReachableNames
            dce.apply(program.globalBlock)

            for ((file, block) in inputFiles.zip(blocks)) {
                with(File(file.outputPath)) {
                    parentFile.mkdirs()
                    writeText(block.toString())
                }
            }

            return DeadCodeEliminationResult(dce.reachableNodes)
        }

        private val reporter = object : ErrorReporter {
            override fun warning(message: String, startPosition: CodePosition, endPosition: CodePosition) {
                println("[WARN] at ${startPosition.line}, ${startPosition.offset}: $message")
            }

            override fun error(message: String, startPosition: CodePosition, endPosition: CodePosition) {
                println("[ERRO] at ${startPosition.line}, ${startPosition.offset}: $message")
            }
        }
    }
}