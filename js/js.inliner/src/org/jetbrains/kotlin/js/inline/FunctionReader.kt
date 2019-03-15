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
import org.jetbrains.kotlin.js.backend.ast.metadata.*
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.inline.util.*
import org.jetbrains.kotlin.js.parser.OffsetToSourceMapping
import org.jetbrains.kotlin.js.parser.parseFunction
import org.jetbrains.kotlin.js.parser.sourcemaps.*
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.expression.InlineMetadata
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils.getModuleName
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.inline.InlineStrategy
import org.jetbrains.kotlin.utils.JsLibraryUtils
import java.io.File
import java.io.StringReader

// TODO: add hash checksum to defineModule?
/**
 * Matches string like Kotlin.defineModule("stdlib", _)
 * Kotlin, _ can be renamed by minifier, quotes type can be changed too (" to ')
 */
private val JS_IDENTIFIER_START = "\\p{Lu}\\p{Ll}\\p{Lt}\\p{Lm}\\p{Lo}\\p{Nl}\\\$_"
private val JS_IDENTIFIER_PART = "$JS_IDENTIFIER_START\\p{Pc}\\p{Mc}\\p{Mn}\\d"
private val JS_IDENTIFIER = "[$JS_IDENTIFIER_START][$JS_IDENTIFIER_PART]*"
private val DEFINE_MODULE_PATTERN =
    ("($JS_IDENTIFIER)\\.defineModule\\(\\s*(['\"])([^'\"]+)\\2\\s*,\\s*(\\w+)\\s*\\)").toRegex().toPattern()
private val DEFINE_MODULE_FIND_PATTERN = ".defineModule("

private val specialFunctions = enumValues<SpecialFunction>().joinToString("|") { it.suggestedName }
private val specialFunctionsByName = enumValues<SpecialFunction>().associateBy { it.suggestedName }
private val SPECIAL_FUNCTION_PATTERN = Regex("var\\s+($JS_IDENTIFIER)\\s*=\\s*($JS_IDENTIFIER)\\.($specialFunctions)\\s*;").toPattern()

class FunctionReader(
    private val reporter: JsConfig.Reporter,
    private val config: JsConfig
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
        val specialFunctions: Map<String, SpecialFunction>,
        offsetToSourceMappingProvider: () -> OffsetToSourceMapping,
        val sourceMap: SourceMap?,
        val outputDir: File?
    ) {
        val offsetToSourceMapping by lazy(offsetToSourceMappingProvider)

        val wrapFunctionRegex = specialFunctions.entries
            .singleOrNull { (_, v) -> v == SpecialFunction.WRAP_FUNCTION }?.key
            ?.let { Regex("\\s*$it\\s*\\(\\s*").toPattern() }
    }

    private val moduleNameToInfo by lazy {
        val result = HashMultimap.create<String, ModuleInfo>()

        JsLibraryUtils.traverseJsLibraries(config.libraries.map(::File)) { (content, path, sourceMapContent, file) ->
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

                val matcher = SPECIAL_FUNCTION_PATTERN.matcher(content)
                val specialFunctions = mutableMapOf<String, SpecialFunction>()
                while (matcher.find()) {
                    if (matcher.group(2) == kotlinVariable) {
                        specialFunctions[matcher.group(1)] = specialFunctionsByName[matcher.group(3)]!!
                    }
                }

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
                    specialFunctions = specialFunctions,
                    offsetToSourceMappingProvider = { OffsetToSourceMapping(content) },
                    sourceMap = sourceMap,
                    outputDir = file?.parentFile
                )

                result.put(moduleName, moduleInfo)
            }
        }

        result
    }

    private val shouldRemapPathToRelativeForm = config.shouldGenerateRelativePathsInSourceMap()
    private val relativePathCalculator = config.configuration[JSConfigurationKeys.OUTPUT_DIR]?.let { RelativePathCalculator(it) }

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

    object NotFoundMarker

    private val functionCache = object : SLRUCache<CallableDescriptor, Any>(50, 50) {
        override fun createValue(key: CallableDescriptor): Any =
            readFunction(key) ?: NotFoundMarker
    }

    operator fun get(descriptor: CallableDescriptor, callsiteFragment: JsProgramFragment): FunctionWithWrapper? {
        return functionCache.get(descriptor).let {
            if (it === NotFoundMarker) null else {
                val (fn, info) = it as Pair<*, *>
                renameModules(descriptor, (fn as FunctionWithWrapper).deepCopy(), info as ModuleInfo, callsiteFragment)
            }
        }
    }

    private fun FunctionWithWrapper.deepCopy(): FunctionWithWrapper {
        return if (wrapperBody == null) {
            FunctionWithWrapper(function.deepCopy(), null)
        } else {
            val newWrapper = wrapperBody.deepCopy()
            val newFunction = (newWrapper.statements.last() as JsReturn).expression as JsFunction
            FunctionWithWrapper(newFunction, newWrapper)
        }
    }

    private fun renameModules(
        descriptor: CallableDescriptor,
        fn: FunctionWithWrapper,
        info: ModuleInfo,
        fragment: JsProgramFragment
    ): FunctionWithWrapper {
        val tag = Namer.getFunctionTag(descriptor, config)
        val moduleReference = fragment.inlineModuleMap[tag]?.deepCopy() ?: fragment.scope.declareName("_").makeRef()
        val allDefinedNames = collectDefinedNamesInAllScopes(fn.function)
        val replacements = hashMapOf(
            info.moduleVariable to moduleReference,
            info.kotlinVariable to Namer.kotlinObject()
        )
        replaceExternalNames(fn.function, replacements, allDefinedNames)
        val wrapperStatements = fn.wrapperBody?.statements?.filter { it !is JsReturn }
        wrapperStatements?.forEach { replaceExternalNames(it, replacements, allDefinedNames) }

        return fn
    }

    private fun readFunction(descriptor: CallableDescriptor): Pair<FunctionWithWrapper, ModuleInfo>? {
        val moduleName = getModuleName(descriptor)

        if (moduleName !in moduleNameToInfo.keys()) return null

        for (info in moduleNameToInfo[moduleName]) {
            val function = readFunctionFromSource(descriptor, info)
            if (function != null) return function to info
        }

        return null
    }

    private fun readFunctionFromSource(descriptor: CallableDescriptor, info: ModuleInfo): FunctionWithWrapper? {
        val source = info.fileContent
        var tag = Namer.getFunctionTag(descriptor, config)
        var index = source.indexOf(tag)

        // Hack for compatibility with old versions of stdlib
        // TODO: remove in 1.2
        if (index < 0 && tag == "kotlin.untypedCharArrayF") {
            tag = "kotlin.charArrayF"
            index = source.indexOf(tag)
        }

        if (index < 0) return null

        // + 1 for closing quote
        var offset = index + tag.length + 1
        while (offset < source.length && source[offset].isWhitespaceOrComma) {
            offset++
        }

        val sourcePart = ShallowSubSequence(source, offset, source.length)
        val wrapFunctionMatcher = info.wrapFunctionRegex?.matcher(sourcePart)
        val isWrapped = wrapFunctionMatcher?.lookingAt() == true
        if (isWrapped) {
            offset += wrapFunctionMatcher!!.end()
        }

        val position = info.offsetToSourceMapping[offset]
        val jsScope = JsRootScope(JsProgram())
        val functionExpr = parseFunction(source, info.filePath, position, offset, ThrowExceptionOnErrorReporter, jsScope) ?: return null
        functionExpr.fixForwardNameReferences()
        val (function, wrapper) = if (isWrapped) {
            InlineMetadata.decomposeWrapper(functionExpr) ?: return null
        } else {
            FunctionWithWrapper(functionExpr, null)
        }
        val wrapperStatements = wrapper?.statements?.filter { it !is JsReturn }

        val sourceMap = info.sourceMap
        if (sourceMap != null) {
            val remapper = SourceMapLocationRemapper(sourceMap) {
                remapPath(removeRedundantPathPrefix(it), info)
            }
            remapper.remap(function)
            wrapperStatements?.forEach { remapper.remap(it) }
        }

        val allDefinedNames = collectDefinedNamesInAllScopes(function)

        function.markInlineArguments(descriptor)
        markDefaultParams(function)
        markSpecialFunctions(function, allDefinedNames, info, jsScope)

        val namesWithoutSideEffects = wrapperStatements.orEmpty().asSequence()
            .flatMap { collectDefinedNames(it).asSequence() }
            .toSet()
        function.accept(object : RecursiveJsVisitor() {
            override fun visitNameRef(nameRef: JsNameRef) {
                if (nameRef.name in namesWithoutSideEffects && nameRef.qualifier == null) {
                    nameRef.sideEffects = SideEffectKind.PURE
                }
                super.visitNameRef(nameRef)
            }
        })

        wrapperStatements?.forEach {
            if (it is JsVars && it.vars.size == 1 && extractImportTag(it.vars[0]) != null) {
                it.vars[0].name.imported = true
            }
        }

        return FunctionWithWrapper(function, wrapper)
    }

    private fun markSpecialFunctions(function: JsFunction, allDefinedNames: Set<JsName>, info: ModuleInfo, scope: JsScope) {
        for (externalName in (collectReferencedNames(function) - allDefinedNames)) {
            info.specialFunctions[externalName.ident]?.let {
                externalName.specialFunction = it
            }
        }

        function.body.accept(object : RecursiveJsVisitor() {
            override fun visitNameRef(nameRef: JsNameRef) {
                super.visitNameRef(nameRef)
                markQualifiedSpecialFunction(nameRef)
            }

            private fun markQualifiedSpecialFunction(nameRef: JsNameRef) {
                val qualifier = nameRef.qualifier as? JsNameRef ?: return
                if (qualifier.ident != info.kotlinVariable || qualifier.qualifier != null) return
                if (nameRef.name?.specialFunction != null) return

                val specialFunction = specialFunctionsByName[nameRef.ident] ?: return
                if (nameRef.name == null) {
                    nameRef.name = scope.declareName(nameRef.ident)
                }
                nameRef.name!!.specialFunction = specialFunction
            }
        })
    }

    private fun markDefaultParams(function: JsFunction) {
        val paramsByNames = function.parameters.associate { it.name to it }
        for (ifStatement in function.body.statements) {
            if (ifStatement !is JsIf || ifStatement.elseStatement != null) break
            val thenStatement = ifStatement.thenStatement as? JsExpressionStatement ?: break
            val testExpression = ifStatement.ifExpression as? JsBinaryOperation ?: break

            if (testExpression.operator != JsBinaryOperator.REF_EQ) break
            val testLhs = testExpression.arg1 as? JsNameRef ?: break
            val param = paramsByNames[testLhs.name] ?: break
            if (testLhs.qualifier != null) break
            if ((testExpression.arg2 as? JsPrefixOperation)?.operator != JsUnaryOperator.VOID) break

            val (assignLhs) = JsAstUtils.decomposeAssignmentToVariable(thenStatement.expression) ?: break
            if (assignLhs != testLhs.name) break

            param.hasDefaultValue = true
        }
    }

    private fun removeRedundantPathPrefix(path: String): String {
        var index = 0
        while (index + 2 <= path.length && path.substring(index, index + 2) == "./") {
            index += 2
            while (index < path.length && path[index] == '/') {
                ++index
            }
        }

        return path.substring(index)
    }

    private fun remapPath(path: String, info: ModuleInfo): String {
        if (!shouldRemapPathToRelativeForm) return path
        val outputDir = info.outputDir ?: return path
        val calculator = relativePathCalculator ?: return path
        return calculator.calculateRelativePathTo(File(outputDir, path)) ?: path
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

    val visitor = object : JsVisitorWithContextImpl() {
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

private fun replaceExternalNames(node: JsNode, replacements: Map<String, JsExpression>, definedNames: Set<JsName>) {
    val visitor = object : JsVisitorWithContextImpl() {
        override fun endVisit(x: JsNameRef, ctx: JsContext<JsNode>) {
            if (x.qualifier != null || x.name in definedNames) return

            replacements[x.ident]?.let {
                ctx.replaceMe(it)
            }
        }
    }

    visitor.accept(node)
}

private class ShallowSubSequence(private val underlying: CharSequence, private val start: Int, end: Int) : CharSequence {
    override val length: Int = end - start

    override fun get(index: Int): Char {
        if (index !in 0 until length) throw IndexOutOfBoundsException("$index is out of bounds 0..$length")
        return underlying[index + start]
    }

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
        ShallowSubSequence(underlying, start + startIndex, start + endIndex)
}