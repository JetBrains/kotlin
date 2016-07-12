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

import com.google.dart.compiler.backend.js.ast.*
import com.google.dart.compiler.backend.js.ast.metadata.inlineStrategy
import com.google.gwt.dev.js.ThrowExceptionOnErrorReporter
import com.intellij.util.containers.SLRUCache
import org.jetbrains.kotlin.builtins.isExtensionFunctionType
import org.jetbrains.kotlin.builtins.isFunctionTypeOrSubtype
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.js.config.LibrarySourcesConfig
import org.jetbrains.kotlin.js.inline.util.IdentitySet
import org.jetbrains.kotlin.js.inline.util.isCallInvocation
import org.jetbrains.kotlin.js.parser.parseFunction
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.reference.CallExpressionTranslator
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils.getExternalModuleName
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.inline.InlineStrategy
import org.jetbrains.kotlin.utils.JsLibraryUtils
import org.jetbrains.kotlin.utils.sure
import java.io.File

// TODO: add hash checksum to defineModule?
/**
 * Matches string like Kotlin.defineModule("stdlib", _)
 * Kotlin, _ can be renamed by minifier, quotes type can be changed too (" to ')
 */
private val JS_IDENTIFIER_START = "\\p{Lu}\\p{Ll}\\p{Lt}\\p{Lm}\\p{Lo}\\p{Nl}\\\$_"
private val JS_IDENTIFIER_PART = "$JS_IDENTIFIER_START\\p{Pc}\\p{Mc}\\p{Mn}\\d"
private val JS_IDENTIFIER="[$JS_IDENTIFIER_START][$JS_IDENTIFIER_PART]*"
private val DEFINE_MODULE_PATTERN = ("($JS_IDENTIFIER)\\.defineModule\\(\\s*(['\"])(\\w+)\\2\\s*,\\s*(\\w+)\\s*\\)").toRegex().toPattern()
private val DEFINE_MODULE_FIND_PATTERN = ".defineModule("

class FunctionReader(private val context: TranslationContext) {
    /**
     * Maps module name to .js file content, that contains this module definition.
     * One file can contain more than one module definition.
     */
    private val moduleJsDefinition = hashMapOf<String, String>()

    /**
     * Maps module name to variable, that is used to call functions inside module.
     * The default variable is _, but it can be renamed by minifier.
     */
    private val moduleRootVariable = hashMapOf<String, String>()

    /**
     * Maps moduleName to kotlin object variable.
     * The default variable is Kotlin, but it can be renamed by minifier.
     */
    private val moduleKotlinVariable = hashMapOf<String, String>()

    init {
        val config = context.config as LibrarySourcesConfig
        val libs = config.libraries.map { File(it) }

        JsLibraryUtils.traverseJsLibraries(libs) { fileContent, path ->
            var current = 0

            while (true) {
                var index = fileContent.indexOf(DEFINE_MODULE_FIND_PATTERN, current)
                if (index < 0) break

                current = index + 1
                index = rewindToIdentifierStart(fileContent, index)
                val preciseMatcher = DEFINE_MODULE_PATTERN.matcher(offset(fileContent, index))
                if (!preciseMatcher.lookingAt()) continue

                val moduleName = preciseMatcher.group(3)
                val moduleVariable = preciseMatcher.group(4)
                val kotlinVariable = preciseMatcher.group(1)
                assert(moduleName !in moduleJsDefinition) { "Module is defined in more, than one file" }
                moduleJsDefinition[moduleName] = fileContent
                moduleRootVariable[moduleName] = moduleVariable
                moduleKotlinVariable[moduleName] = kotlinVariable
            }
        }
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
        val moduleName = getExternalModuleName(descriptor)
        val currentModuleName = context.config.moduleId
        return currentModuleName != moduleName && moduleName != null && moduleName in moduleJsDefinition
    }

    operator fun get(descriptor: CallableDescriptor): JsFunction = functionCache.get(descriptor)
    
    private fun readFunction(descriptor: CallableDescriptor): JsFunction? {
        if (descriptor !in this) return null

        val moduleName = getExternalModuleName(descriptor)
        val file = moduleJsDefinition[moduleName].sure { "Module $moduleName file have not been read" }
        val function = readFunctionFromSource(descriptor, file)
        function?.markInlineArguments(descriptor)
        return function
    }

    private fun readFunctionFromSource(descriptor: CallableDescriptor, source: String): JsFunction? {
        val tag = Namer.getFunctionTag(descriptor)
        val index = source.indexOf(tag)
        if (index < 0) return null

        // + 1 for closing quote
        var offset = index + tag.length + 1
        while (offset < source.length && source[offset].isWhitespaceOrComma) {
            offset++
        }

        val function = parseFunction(source, offset, ThrowExceptionOnErrorReporter, JsRootScope(JsProgram("<inline>")))
        val moduleName = getExternalModuleName(descriptor)!!
        val moduleReference = context.getModuleExpressionFor(descriptor) ?: getRootPackage()

        val replacements = hashMapOf(moduleRootVariable[moduleName]!! to moduleReference,
                                     moduleKotlinVariable[moduleName]!! to Namer.kotlinObject())
        replaceExternalNames(function, replacements)
        return function
    }

    private fun getRootPackage(): JsExpression {
        val rootName = context.program().rootScope.declareName(Namer.getRootPackageName())
        return JsAstUtils.fqnWithoutSideEffects(rootName, null)
    }
}

private val Char.isWhitespaceOrComma: Boolean
    get() = this == ',' || this.isWhitespace()

private fun JsFunction.markInlineArguments(descriptor: CallableDescriptor) {
    val params = descriptor.valueParameters
    val paramsJs = parameters
    val inlineFuns = IdentitySet<JsName>()
    val inlineExtensionFuns = IdentitySet<JsName>()
    val offset = if (descriptor.isExtension) 1 else 0

    for ((i, param) in params.withIndex()) {
        if (!CallExpressionTranslator.shouldBeInlined(descriptor)) continue

        val type = param.type
        if (!type.isFunctionTypeOrSubtype) continue

        val namesSet = if (type.isExtensionFunctionType) inlineExtensionFuns else inlineFuns
        namesSet.add(paramsJs[i + offset].name)
    }

    val visitor = object: JsVisitorWithContextImpl() {
        override fun endVisit(x: JsInvocation, ctx: JsContext<*>) {
            val qualifier: JsExpression?
            val namesSet: Set<JsName>

            if (isCallInvocation(x)) {
                qualifier = (x.qualifier as? JsNameRef)?.qualifier
                namesSet = inlineExtensionFuns
            } else {
                qualifier = x.qualifier
                namesSet = inlineFuns
            }

            (qualifier as? JsNameRef)?.name?.let { name ->
                if (name in namesSet) {
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
