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

package org.jetbrains.kotlin.js.inline

import com.google.common.collect.HashMultimap
import com.google.gwt.dev.js.ThrowExceptionOnErrorReporter
import com.intellij.util.containers.SLRUCache
import org.jetbrains.kotlin.builtins.isFunctionTypeOrSubtype
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.inlineStrategy
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.inline.util.IdentitySet
import org.jetbrains.kotlin.js.inline.util.isCallInvocation
import org.jetbrains.kotlin.js.parser.OffsetToSourceMapping
import org.jetbrains.kotlin.js.parser.parseFunction
import org.jetbrains.kotlin.js.parser.sourcemaps.*
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils.getModuleName
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.inline.InlineStrategy
import org.jetbrains.kotlin.utils.JsLibraryUtils
import org.jetbrains.kotlin.utils.sure
import java.io.File
import java.io.StringReader

// TODO: add hash checksum to defineModule?
/**
 * Matches string like Kotlin.defineModule("stdlib", _)
 * Kotlin, _ can be renamed by minifier, quotes type can be changed too (" to ')
 */
private val JS_IDENTIFIER_START = "\\p{Lu}\\p{Ll}\\p{Lt}\\p{Lm}\\p{Lo}\\p{Nl}\\\$_"
private val JS_IDENTIFIER_PART = "$JS_IDENTIFIER_START\\p{Pc}\\p{Mc}\\p{Mn}\\d"
private val JS_IDENTIFIER="[$JS_IDENTIFIER_START][$JS_IDENTIFIER_PART]*"
private val DEFINE_MODULE_PATTERN = ("($JS_IDENTIFIER)\\.defineModule\\(\\s*(['\"])([^'\"]+)\\2\\s*,\\s*(\\w+)\\s*\\)").toRegex().toPattern()
private val DEFINE_MODULE_FIND_PATTERN = ".defineModule("

class FunctionReader(private val config: JsConfig, private val currentModuleName: JsName, fragments: List<JsProgramFragment>) {
    /**
     * fileContent: .js file content, that contains this module definition.
     *     One file can contain more than one module definition.
     *
     * moduleVariable: the variable used to call functions inside module.
     *     The default variable is _, but it can be renamed by minifier.
     *
     * kotlinVariable: kotlin object variable.
     *     The default variable is Kotlin, but it can be renamed by minifier.
     */
    class ModuleInfo(
            val filePath: String,
            val fileContent: String,
            val moduleVariable: String,
            val kotlinVariable: String,
            val offsetToSourceMapping: OffsetToSourceMapping,
            val sourceMap: SourceMap?
    )

    private val moduleNameToInfo = HashMultimap.create<String, ModuleInfo>()

    private val moduleNameMap: Map<String, JsExpression>

    init {
        val libs = config.libraries.map(::File)

        moduleNameMap = buildModuleNameMap(fragments)

        JsLibraryUtils.traverseJsLibraries(libs) { (content, path, sourceMapContent) ->
            var current = 0

            while (true) {
                var index = content.indexOf(DEFINE_MODULE_FIND_PATTERN, current)
                if (index < 0) break

                current = index + 1
                index = rewindToIdentifierStart(content, index)
                val preciseMatcher = DEFINE_MODULE_PATTERN.matcher(offset(content, index))
                if (!preciseMatcher.lookingAt()) continue

                val moduleName = preciseMatcher.group(3)
                val moduleVariable = preciseMatcher.group(4)
                val kotlinVariable = preciseMatcher.group(1)

                val sourceMap = sourceMapContent?.let {
                    val result = SourceMapParser.parse(StringReader(it))
                    when (result) {
                        is SourceMapSuccess -> result.value
                        is SourceMapError -> throw RuntimeException("Error parsing source map file: ${result.message}\n$it")
                    }
                }

                val moduleInfo = ModuleInfo(
                        filePath = path,
                        fileContent = content,
                        moduleVariable = moduleVariable,
                        kotlinVariable = kotlinVariable,
                        offsetToSourceMapping = OffsetToSourceMapping(content),
                        sourceMap = sourceMap
                )

                moduleNameToInfo.put(moduleName, moduleInfo)
            }
        }
    }

    // Since we compile each source file in its own context (and we may loose these context when performing incremental compilation)
    // we don't use contexts to generate proper names for modules. Instead, we generate all necessary information during
    // translation and rely on it here.
    private fun buildModuleNameMap(fragments: List<JsProgramFragment>): Map<String, JsExpression> {
        return fragments.flatMap { it.inlineModuleMap.entries }.associate { (k, v) -> k to v }
    }

    private fun rewindToIdentifierStart(text: String, index: Int): Int {
        var result = index
        while (result > 0 && Character.isJavaIdentifierPart(text[result - 1])) {
            --result
        }
        return result
    }

    private fun offset(text: String, offset: Int) = object : CharSequence {
        override val length: Int
            get() = text.length - offset

        override fun get(index: Int) = text[index + offset]

        override fun subSequence(startIndex: Int, endIndex: Int) = text.subSequence(startIndex + offset, endIndex + offset)

        override fun toString() = text.substring(offset)
    }

    private val functionCache = object : SLRUCache<CallableDescriptor, JsFunction>(50, 50) {
        override fun createValue(descriptor: CallableDescriptor): JsFunction =
                readFunction(descriptor).sure { "Could not read function: $descriptor" }
    }

    operator fun contains(descriptor: CallableDescriptor): Boolean {
        val moduleName = getModuleName(descriptor)
        val currentModuleName = config.moduleId
        return currentModuleName != moduleName && moduleName in moduleNameToInfo.keys()
    }

    operator fun get(descriptor: CallableDescriptor): JsFunction = functionCache.get(descriptor)

    private fun readFunction(descriptor: CallableDescriptor): JsFunction? {
        if (descriptor !in this) return null

        val moduleName = getModuleName(descriptor)

        for (info in moduleNameToInfo[moduleName]) {
            val function = readFunctionFromSource(descriptor, info)
            if (function != null) return function
        }

        return null
    }

    private fun readFunctionFromSource(descriptor: CallableDescriptor, info: ModuleInfo): JsFunction? {
        val source = info.fileContent
        val tag = Namer.getFunctionTag(descriptor, config)
        val index = source.indexOf(tag)
        if (index < 0) return null

        // + 1 for closing quote
        var offset = index + tag.length + 1
        while (offset < source.length && source[offset].isWhitespaceOrComma) {
            offset++
        }

        val position = info.offsetToSourceMapping[offset]
        val function = parseFunction(source, info.filePath, position, offset, ThrowExceptionOnErrorReporter, JsRootScope(JsProgram()))
        val moduleReference = moduleNameMap[tag] ?: currentModuleName.makeRef()

        val sourceMap = info.sourceMap
        if (sourceMap != null) {
            val remapper = SourceMapLocationRemapper(mapOf(info.filePath to sourceMap))
            remapper.remap(function)
        }

        val replacements = hashMapOf(info.moduleVariable to moduleReference,
                                     info.kotlinVariable to Namer.kotlinObject())
        replaceExternalNames(function, replacements)
        function.markInlineArguments(descriptor)
        return function
    }
}

private val Char.isWhitespaceOrComma: Boolean
    get() = this == ',' || this.isWhitespace()

private fun JsFunction.markInlineArguments(descriptor: CallableDescriptor) {
    val params = descriptor.valueParameters
    val paramsJs = parameters
    val inlineFuns = IdentitySet<JsName>()
    val offset = if (descriptor.isExtension) 1 else 0

    for ((i, param) in params.withIndex()) {
        val type = param.type
        if (!type.isFunctionTypeOrSubtype) continue

        inlineFuns.add(paramsJs[i + offset].name)
    }

    val visitor = object: JsVisitorWithContextImpl() {
        override fun endVisit(x: JsInvocation, ctx: JsContext<*>) {
            val qualifier: JsExpression?

            if (isCallInvocation(x)) {
                qualifier = (x.qualifier as? JsNameRef)?.qualifier
            } else {
                qualifier = x.qualifier
            }

            (qualifier as? JsNameRef)?.name?.let { name ->
                if (name in inlineFuns) {
                    x.inlineStrategy = InlineStrategy.IN_PLACE
                }
            }
        }
    }

    visitor.accept(this)
}

private fun replaceExternalNames(function: JsFunction, externalReplacements: Map<String, JsExpression>) {
    val replacements = externalReplacements.filterKeys { !function.scope.hasOwnName(it) }

    if (replacements.isEmpty()) return

    val visitor = object: JsVisitorWithContextImpl() {
        override fun endVisit(x: JsNameRef, ctx: JsContext<JsNode>) {
            if (x.qualifier != null) return

            replacements[x.ident]?.let {
                ctx.replaceMe(it)
            }
        }
    }

    visitor.accept(function)
}
