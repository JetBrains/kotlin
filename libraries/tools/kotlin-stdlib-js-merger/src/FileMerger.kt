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

package org.jetbrains.kotlin.js

import com.google.gwt.dev.js.ThrowExceptionOnErrorReporter
import org.jetbrains.kotlin.js.backend.JsToStringGenerationVisitor
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.facade.SourceMapBuilderConsumer
import org.jetbrains.kotlin.js.inline.util.fixForwardNameReferences
import org.jetbrains.kotlin.js.parser.parse
import org.jetbrains.kotlin.js.parser.sourcemaps.SourceMapError
import org.jetbrains.kotlin.js.parser.sourcemaps.SourceMapLocationRemapper
import org.jetbrains.kotlin.js.parser.sourcemaps.SourceMapParser
import org.jetbrains.kotlin.js.parser.sourcemaps.SourceMapSuccess
import org.jetbrains.kotlin.js.sourceMap.SourceFilePathResolver
import org.jetbrains.kotlin.js.sourceMap.SourceMap3Builder
import org.jetbrains.kotlin.js.util.TextOutputImpl
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.io.StringReader

fun main(args: Array<String>) {
    val program = JsProgram()

    val outputFile = File(args[0])
    val baseDir = File(args[1]).canonicalFile.toPath()
    fun File.relativizeIfNecessary(): String = baseDir.relativize(canonicalFile.toPath()).toString()

    val wrapperFile = File(args[2])
    val wrapper = parse(wrapperFile.readText(), ThrowExceptionOnErrorReporter, program.scope, wrapperFile.relativizeIfNecessary())!!
    val insertionPlace = wrapper.createInsertionPlace()

    val allFiles = mutableListOf<File>()
    args.drop(3).map { File(it) }.forEach { collectFiles(it, allFiles) }

    for (file in allFiles) {
        val statements = parse(file.readText(), ThrowExceptionOnErrorReporter, program.scope, file.relativizeIfNecessary())!!
        val block = JsBlock(statements)
        block.fixForwardNameReferences()

        val sourceMapFile = File(file.parent, file.name + ".map")
        if (sourceMapFile.exists()) {
            val sourceMapParse = sourceMapFile.reader().use { SourceMapParser.parse(it) }
            when (sourceMapParse) {
                is SourceMapError -> {
                    System.err.println("Error parsing source map file $sourceMapFile: ${sourceMapParse.message}")
                    System.exit(1)
                }
                is SourceMapSuccess -> {
                    val sourceMap = sourceMapParse.value
                    val remapper = SourceMapLocationRemapper(sourceMap)
                    remapper.remap(block)
                }
            }
        }

        insertionPlace.statements += statements
    }

    program.globalBlock.statements += wrapper

    val sourceMapFile = File(outputFile.parentFile, outputFile.name + ".map")
    val textOutput = TextOutputImpl()
    val sourceMapBuilder = SourceMap3Builder(outputFile, textOutput, "")
    val consumer = SourceMapBuilderConsumer(sourceMapBuilder, SourceFilePathResolver(mutableListOf()), true, true)
    program.globalBlock.accept(JsToStringGenerationVisitor(textOutput, consumer))
    val sourceMapContent = sourceMapBuilder.build()

    val programText = textOutput.toString()

    outputFile.writeText(programText + "\n//# sourceMappingURL=${sourceMapFile.name}\n")

    val sourceMapJson = StringReader(sourceMapContent).use { JSONObject(JSONTokener(it)) }
    val sources = sourceMapJson["sources"] as JSONArray

    sourceMapJson.put("sourcesContent", sources.map { sourcePath ->
        val sourceFile = File(sourcePath as String)
        if (sourceFile.exists()) {
            sourceFile.readText()
        }
        else {
            null
        }
    })

    sourceMapFile.writeText(sourceMapJson.toString(2))
}

private fun List<JsStatement>.createInsertionPlace(): JsBlock {
    val block = JsGlobalBlock()

    val visitor = object : JsVisitorWithContextImpl() {
        override fun visit(x: JsExpressionStatement, ctx: JsContext<in JsStatement>): Boolean {
            if (isInsertionPlace(x.expression)) {
                ctx.replaceMe(block)
                return false
            }
            else {
                return super.visit(x, ctx)
            }
        }

        private fun isInsertionPlace(expression: JsExpression): Boolean {
            if (expression !is JsInvocation || !expression.arguments.isEmpty()) return false

            val qualifier = expression.qualifier
            if (qualifier !is JsNameRef || qualifier.qualifier != null) return false
            return qualifier.ident == "insertContent"
        }
    }

    for (statement in this) {
        visitor.accept(statement)
    }
    return block
}

private fun collectFiles(rootFile: File, target: MutableList<File>) {
    if (rootFile.isDirectory) {
        for (child in rootFile.listFiles().sorted()) {
            collectFiles(child, target)
        }
    }
    else if (rootFile.extension == "js") {
        target += rootFile
    }
}