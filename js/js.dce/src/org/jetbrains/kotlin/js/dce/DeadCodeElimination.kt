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
import org.jetbrains.kotlin.js.backend.JsToStringGenerationVisitor
import org.jetbrains.kotlin.js.backend.ast.JsBlock
import org.jetbrains.kotlin.js.backend.ast.JsGlobalBlock
import org.jetbrains.kotlin.js.backend.ast.JsNode
import org.jetbrains.kotlin.js.backend.ast.JsProgram
import org.jetbrains.kotlin.js.dce.Context.Node
import org.jetbrains.kotlin.js.facade.SourceMapBuilderConsumer
import org.jetbrains.kotlin.js.inline.util.collectDefinedNames
import org.jetbrains.kotlin.js.inline.util.fixForwardNameReferences
import org.jetbrains.kotlin.js.parser.parse
import org.jetbrains.kotlin.js.parser.sourcemaps.SourceMapError
import org.jetbrains.kotlin.js.parser.sourcemaps.SourceMapLocationRemapper
import org.jetbrains.kotlin.js.parser.sourcemaps.SourceMapParser
import org.jetbrains.kotlin.js.parser.sourcemaps.SourceMapSuccess
import org.jetbrains.kotlin.js.sourceMap.SourceFilePathResolver
import org.jetbrains.kotlin.js.sourceMap.SourceMap3Builder
import org.jetbrains.kotlin.js.util.TextOutputImpl
import java.io.File
import java.io.InputStreamReader

class DeadCodeElimination(private val logConsumer: (DCELogLevel, String) -> Unit) {
    val moduleMapping = mutableMapOf<JsBlock, String>()
    private val reachableNames = mutableSetOf<String>()

    var reachableNodes = setOf<Node>()
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
                logConsumer: (DCELogLevel, String) -> Unit
        ): DeadCodeEliminationResult {
            val program = JsProgram()
            val dce = DeadCodeElimination(logConsumer)

            var hasErrors = false
            val blocks = inputFiles.map { file ->
                val block = JsGlobalBlock()
                val code = file.resource.reader().let { InputStreamReader(it, "UTF-8") }.use { it.readText() }
                val statements = parse(code, Reporter(file.resource.name, logConsumer), program.scope, file.resource.name) ?: run {
                    hasErrors = true
                    return@map block
                }
                val sourceMapParse = file.sourceMapResource
                        ?.let { SourceMapParser.parse(InputStreamReader(it.reader(), "UTF-8").readText()) }
                when (sourceMapParse) {
                    is SourceMapError -> {
                        logConsumer(
                                DCELogLevel.WARN,
                                "Error parsing source map file ${file.sourceMapResource}: ${sourceMapParse.message}")
                    }
                    is SourceMapSuccess -> {
                        val sourceMap = sourceMapParse.value
                        val remapper = SourceMapLocationRemapper(sourceMap)
                        statements.forEach { remapper.remap(it) }
                    }
                }
                block.statements += statements
                file.moduleName?.let { dce.moduleMapping[block] = it }
                block
            }

            if (hasErrors) return DeadCodeEliminationResult(emptySet(), DeadCodeEliminationStatus.FAILED)

            program.globalBlock.statements += blocks
            program.globalBlock.fixForwardNameReferences()

            dce.reachableNames += rootReachableNames
            dce.apply(program.globalBlock)

            for ((file, block) in inputFiles.zip(blocks)) {
                val sourceMapFile = File(file.outputPath + ".map")
                val textOutput = TextOutputImpl()
                val sourceMapBuilder = SourceMap3Builder(File(file.outputPath), textOutput, "")

                val inputFile = File(file.resource.name)
                val sourceBaseDir = if (inputFile.exists()) inputFile.parentFile else File(".")

                val sourcePathResolver = SourceFilePathResolver(emptyList(), File(file.outputPath).parentFile)
                val consumer = SourceMapBuilderConsumer(sourceBaseDir, sourceMapBuilder, sourcePathResolver, true, true)
                block.accept(JsToStringGenerationVisitor(textOutput, consumer))
                val sourceMapContent = sourceMapBuilder.build()
                sourceMapBuilder.addLink()

                with(File(file.outputPath)) {
                    parentFile.mkdirs()
                    writeText(textOutput.toString())
                }

                if (file.sourceMapResource != null) {
                    sourceMapFile.writeText(sourceMapContent)
                }
            }

            return DeadCodeEliminationResult(dce.reachableNodes, DeadCodeEliminationStatus.OK)
        }

        private class Reporter(private val fileName: String, private val logConsumer: (DCELogLevel, String) -> Unit) : ErrorReporter {
            override fun warning(message: String, startPosition: CodePosition, endPosition: CodePosition) {
                logConsumer(DCELogLevel.WARN, "at $fileName (${startPosition.line + 1}, ${startPosition.offset + 1}): $message")
            }

            override fun error(message: String, startPosition: CodePosition, endPosition: CodePosition) {
                logConsumer(DCELogLevel.ERROR, "at $fileName (${startPosition.line + 1}, ${startPosition.offset + 1}): $message")
            }
        }
    }
}