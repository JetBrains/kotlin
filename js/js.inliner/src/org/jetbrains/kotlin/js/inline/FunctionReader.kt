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
import org.jetbrains.kotlin.js.backend.ast.metadata.SideEffectKind
import org.jetbrains.kotlin.js.backend.ast.metadata.imported
import org.jetbrains.kotlin.js.backend.ast.metadata.inlineStrategy
import org.jetbrains.kotlin.js.backend.ast.metadata.sideEffects
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.inline.util.*
import org.jetbrains.kotlin.js.parser.OffsetToSourceMapping
import org.jetbrains.kotlin.js.parser.parseFunction
import org.jetbrains.kotlin.js.parser.sourcemaps.*
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.expression.InlineMetadata
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

class FunctionReader(
        private val reporter: JsConfig.Reporter,
        private val config: JsConfig,
        private val currentModuleName: JsName,
        fragments: List<JsProgramFragment>
) {
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
            offsetToSourceMappingProvider: () -> OffsetToSourceMapping,
            val sourceMap: SourceMap?
    ) {
        val offsetToSourceMapping by lazy(offsetToSourceMappingProvider)
    }

    private val moduleNameToInfo by lazy {
        val result = HashMultimap.create<String, ModuleInfo>()

        JsLibraryUtils.traverseJsLibraries(config.libraries.map(::File)) { (content, path, sourceMapContent) ->
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
                    val sourceMapResult = SourceMapParser.parse(StringReader(it))
                    when (sourceMapResult) {
                        is SourceMapSuccess -> sourceMapResult.value
                        is SourceMapError -> {
                            reporter.warning("Error parsing source map file for $path: ${sourceMapResult.message}")
                            null
                        }
                    }
                }

                val moduleInfo = ModuleInfo(
                        filePath = path,
                        fileContent = content,
                        moduleVariable = moduleVariable,
                        kotlinVariable = kotlinVariable,
                        offsetToSourceMappingProvider = { OffsetToSourceMapping(content) },
                        sourceMap = sourceMap
                )

                result.put(moduleName, moduleInfo)
            }
        }

        result
    }

    private val moduleNameMap: Map<String, JsExpression>

    init {
        moduleNameMap = buildModuleNameMap(fragments)
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

    private val functionCache = object : SLRUCache<CallableDescriptor, FunctionWithWrapper>(50, 50) {
        override fun createValue(descriptor: CallableDescriptor): FunctionWithWrapper =
                readFunction(descriptor).sure { "Could not read function: $descriptor" }
    }

    operator fun contains(descriptor: CallableDescriptor): Boolean {
        val moduleName = getModuleName(descriptor)
        val currentModuleName = config.moduleId
        return currentModuleName != moduleName && moduleName in moduleNameToInfo.keys()
    }

    operator fun get(descriptor: CallableDescriptor): FunctionWithWrapper = functionCache.get(descriptor)

    private fun readFunction(descriptor: CallableDescriptor): FunctionWithWrapper? {
        if (descriptor !in this) return null

        val moduleName = getModuleName(descriptor)

        for (info in moduleNameToInfo[moduleName]) {
            val function = readFunctionFromSource(descriptor, info)
            if (function != null) return function
        }

        return null
    }

    private fun readFunctionFromSource(descriptor: CallableDescriptor, info: ModuleInfo): FunctionWithWrapper? {
        val source = info.fileContent
        val tag = Namer.getFunctionTag(descriptor, config)
        val index = source.indexOf(tag)
        if (index < 0) return null

        // + 1 for closing quote
        var offset = index + tag.length + 1
        while (offset < source.length && source[offset].isWhitespaceOrComma) {
            offset++
        }

        val wrapFunctionMatcher = wrapFunctionRegex.matcher(ShallowSubSequence(source, offset, source.length))
        val isWrapped = wrapFunctionMatcher.lookingAt()
        if (isWrapped) {
            offset += wrapFunctionMatcher.end()
        }

        val position = info.offsetToSourceMapping[offset]
        val functionExpr = parseFunction(source, info.filePath, position, offset, ThrowExceptionOnErrorReporter, JsRootScope(JsProgram()))
        functionExpr.fixForwardNameReferences()
        val (function, wrapper) = if (isWrapped) {
            InlineMetadata.decomposeWrapper(functionExpr) ?: return null
        }
        else {
            FunctionWithWrapper(functionExpr, null)
        }
        val moduleReference = moduleNameMap[tag]?.deepCopy() ?: currentModuleName.makeRef()
        val wrapperStatements = wrapper?.statements?.filter { it !is JsReturn }

        val sourceMap = info.sourceMap
        if (sourceMap != null) {
            val remapper = SourceMapLocationRemapper(sourceMap)
            remapper.remap(function)
            wrapperStatements?.forEach { remapper.remap(it) }
        }

        val replacements = hashMapOf(info.moduleVariable to moduleReference,
                                     info.kotlinVariable to Namer.kotlinObject())
        replaceExternalNames(function, replacements)
        wrapperStatements?.forEach { replaceExternalNames(it, replacements) }
        function.markInlineArguments(descriptor)

        val namesWithoutSizeEffects = wrapperStatements.orEmpty().asSequence()
                .flatMap { collectDefinedNames(it).asSequence() }
                .toSet()
        function.accept(object : RecursiveJsVisitor() {
            override fun visitNameRef(nameRef: JsNameRef) {
                if (nameRef.name in namesWithoutSizeEffects && nameRef.qualifier == null) {
                    nameRef.sideEffects = SideEffectKind.PURE
                }
                super.visitNameRef(nameRef)
            }
        })

        wrapperStatements?.forEach {
            if (it is JsVars && it.vars.size == 1 && it.vars[0].initExpression?.let { extractImportTag(it) } != null) {
                it.vars[0].name.imported = true
            }
        }

        return FunctionWithWrapper(function, wrapper)
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
            val qualifier: JsExpression? = if (isCallInvocation(x)) {
                (x.qualifier as? JsNameRef)?.qualifier
            } else {
                x.qualifier
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

private fun replaceExternalNames(node: JsNode, replacements: Map<String, JsExpression>) {
    val skipNames = collectDefinedNamesInAllScopes(node)

    val visitor = object: JsVisitorWithContextImpl() {
        override fun endVisit(x: JsNameRef, ctx: JsContext<JsNode>) {
            if (x.qualifier != null || x.name in skipNames) return

            replacements[x.ident]?.let {
                ctx.replaceMe(it)
            }
        }
    }

    visitor.accept(node)
}

private val wrapFunctionRegex = Regex("\\s*[a-zA-Z_$][a-zA-Z0-9_$]*\\s*\\.\\s*wrapFunction\\s*\\(\\s*").toPattern()

private class ShallowSubSequence(private val underlying: CharSequence, private val start: Int, end: Int) : CharSequence {
    override val length: Int = end - start

    override fun get(index: Int): Char {
        if (index !in 0 until length) throw IndexOutOfBoundsException("$index is out of bounds 0..$length")
        return underlying[index + start]
    }

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
            ShallowSubSequence(underlying, start + startIndex, start + endIndex)
}