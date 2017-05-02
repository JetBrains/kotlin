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
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.js.backend.ast.JsBlock
import org.jetbrains.kotlin.js.backend.ast.JsGlobalBlock
import org.jetbrains.kotlin.js.backend.ast.JsNode
import org.jetbrains.kotlin.js.backend.ast.JsProgram
import org.jetbrains.kotlin.js.dce.Context.Node
import org.jetbrains.kotlin.js.inline.util.collectDefinedNames
import org.jetbrains.kotlin.js.inline.util.fixForwardNameReferences
import org.jetbrains.kotlin.js.parser.parse
import java.io.File

object DeadCodeElimination {
    fun dce(root: JsNode, logConsumer: (String) -> Unit, reachableNames: Set<String>, moduleMapping: Map<JsBlock, String>): Set<Node> {
        val context = Context()

        val topLevelVars = collectDefinedNames(root)
        context.addLocalVars(topLevelVars)
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

        Eliminator(analyzer.analysisResult).accept(root)

        return usageFinder.reachableNodes
    }


    fun run(
            inputFiles: Collection<InputFile>,
            rootReachableNames: Set<String>,
            logConsumer: (String) -> Unit
    ): DeadCodeEliminationResult {
        val program = JsProgram()
        val moduleMapping = mutableMapOf<JsBlock, String>()

        val blocks = inputFiles.map { file ->
            val code = FileUtil.loadFile(File(file.name))
            val block = JsGlobalBlock()
            block.statements += parse(code, reporter, program.scope, file.name)
            moduleMapping[block] = file.moduleName
            block
        }

        program.globalBlock.statements += blocks
        program.globalBlock.fixForwardNameReferences()

        val result = dce(program.globalBlock, logConsumer, rootReachableNames, moduleMapping)

        for ((file, block) in inputFiles.zip(blocks)) {
            FileUtil.writeToFile(File(file.outputName), block.toString())
        }

        return DeadCodeEliminationResult(result)
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