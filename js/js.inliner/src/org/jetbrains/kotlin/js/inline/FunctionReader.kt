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
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.containers.SLRUCache
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.js.config.LibrarySourcesConfig
import org.jetbrains.kotlin.js.inline.util.IdentitySet
import org.jetbrains.kotlin.js.inline.util.isCallInvocation
import org.jetbrains.kotlin.js.parser.parseFunction
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.reference.CallExpressionTranslator
import org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils.getExternalModuleName
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.inline.InlineStrategy
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils
import org.jetbrains.kotlin.utils.LibraryUtils
import org.jetbrains.kotlin.utils.sure
import java.io.File
import kotlin.platform.platformStatic

// TODO: add hash checksum to defineModule?
/**
 * Matches string like Kotlin.defineModule("stdlib", _)
 * Kotlin, _ can be renamed by minifier, quotes type can be changed too (" to ')
 */
private val DEFINE_MODULE_PATTERN = "(\\w+)\\.defineModule\\(\\s*(['\"])(\\w+)\\2\\s*,\\s*(\\w+)\\s*\\)".toRegex()

public class FunctionReader(private val context: TranslationContext) {
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
        val config = context.getConfig() as LibrarySourcesConfig
        val libs = config.getLibraries().map { File(it) }

        LibraryUtils.traverseJsLibraries(libs) { fileContent, path ->
            val matcher = DEFINE_MODULE_PATTERN.toPattern().matcher(fileContent)

            while (matcher.find()) {
                val moduleName = matcher.group(3)
                val moduleVariable = matcher.group(4)
                val kotlinVariable = matcher.group(1)
                assert(moduleName !in moduleJsDefinition) { "Module is defined in more, than one file" }
                moduleJsDefinition[moduleName] = fileContent
                moduleRootVariable[moduleName] = moduleVariable
                moduleKotlinVariable[moduleName] = kotlinVariable
            }
        }
    }

    private val functionCache = object : SLRUCache<CallableDescriptor, JsFunction>(50, 50) {
        override fun createValue(descriptor: CallableDescriptor): JsFunction =
                readFunction(descriptor).sure { "Could not read function: $descriptor" }
    }

    public fun contains(descriptor: CallableDescriptor): Boolean {
        val moduleName = getExternalModuleName(descriptor)
        val currentModuleName = context.getConfig().getModuleId()
        return currentModuleName != moduleName && moduleName != null && moduleName in moduleJsDefinition
    }

    public fun get(descriptor: CallableDescriptor): JsFunction = functionCache.get(descriptor)
    
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
        var offset = index + tag.length() + 1
        while (offset < source.length() && source.charAt(offset).isWhitespaceOrComma) {
            offset++
        }

        val function = parseFunction(source, offset, ThrowExceptionOnErrorReporter, JsRootScope(JsProgram("<inline>")))
        val moduleName = getExternalModuleName(descriptor)!!
        val moduleNameLiteral = context.program().getStringLiteral(moduleName)
        val moduleReference =  context.namer().getModuleReference(moduleNameLiteral)

        val replacements = hashMapOf(moduleRootVariable[moduleName]!! to moduleReference,
                                     moduleKotlinVariable[moduleName]!! to Namer.KOTLIN_OBJECT_REF)
        replaceExternalNames(function, replacements)
        return function
    }
}

private val Char.isWhitespaceOrComma: Boolean
    get() = this == ',' || this.isWhitespace()

private fun JsFunction.markInlineArguments(descriptor: CallableDescriptor) {
    val params = descriptor.getValueParameters()
    val paramsJs = getParameters()
    val inlineFuns = IdentitySet<JsName>()
    val inlineExtensionFuns = IdentitySet<JsName>()
    val offset = if (descriptor.isExtension) 1 else 0

    for ((i, param) in params.withIndex()) {
        if (!CallExpressionTranslator.shouldBeInlined(descriptor)) continue

        val type = param.getType()
        if (!KotlinBuiltIns.isFunctionOrExtensionFunctionType(type)) continue

        val namesSet = if (KotlinBuiltIns.isExtensionFunctionType(type)) inlineExtensionFuns else inlineFuns
        namesSet.add(paramsJs[i + offset].getName())
    }

    val visitor = object: JsVisitorWithContextImpl() {
        override fun endVisit(x: JsInvocation, ctx: JsContext<*>) {
            val qualifier: JsExpression?
            val namesSet: Set<JsName>

            if (isCallInvocation(x)) {
                qualifier = (x.getQualifier() as? JsNameRef)?.getQualifier()
                namesSet = inlineExtensionFuns
            } else {
                qualifier = x.getQualifier()
                namesSet = inlineFuns
            }

            val name = (qualifier as? JsNameRef)?.getName()

            if (name in namesSet) {
                x.inlineStrategy = InlineStrategy.IN_PLACE
            }
        }
    }

    visitor.accept(this)
}

private fun replaceExternalNames(function: JsFunction, externalReplacements: Map<String, JsExpression>) {
    val replacements = externalReplacements.filterKeys { !function.getScope().hasOwnName(it) }

    if (replacements.isEmpty()) return

    val visitor = object: JsVisitorWithContextImpl() {
        override fun endVisit(x: JsNameRef, ctx: JsContext<*>) {
            if (x.getQualifier() != null) return

            replacements[x.getIdent()]?.let {
                ctx.replaceMe(it)
            }
        }
    }

    visitor.accept(function)
}
