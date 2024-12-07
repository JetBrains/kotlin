/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.resolve.diagnostics

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.js.naming.JsNameSuggestion
import org.jetbrains.kotlin.js.naming.NameSuggestion
import org.jetbrains.kotlin.js.naming.WasmNameSuggestion
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext

class JsNameCharsChecker(suggestion: JsNameSuggestion) : AbstractNameCharsChecker(suggestion)

class WasmNameCharsChecker(suggestion: WasmNameSuggestion) : AbstractNameCharsChecker(suggestion)

abstract class AbstractNameCharsChecker(private val suggestion: NameSuggestion) : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (context.languageVersionSettings.supportsFeature(LanguageFeature.JsAllowInvalidCharsIdentifiersEscaping)) {
            return
        }
        val bindingContext = context.trace.bindingContext

        if (descriptor is PropertyAccessorDescriptor && AnnotationsUtils.getJsName(descriptor) == null) return

        // This case will be reported as WRONG_EXPORTED_DECLARATION for
        // secondary constructor with missing JsName. Skipping it here to simplify further logic.
        if (descriptor is ConstructorDescriptor &&
            AnnotationsUtils.getJsName(descriptor) == null &&
            AnnotationsUtils.isExportedObject(descriptor, bindingContext)
        ) return

        val suggestedName = suggestion.suggest(descriptor, bindingContext) ?: return
        if (suggestedName.stable && suggestedName.names.any { NameSuggestion.sanitizeName(it) != it }) {
            context.trace.report(ErrorsJs.NAME_CONTAINS_ILLEGAL_CHARS.on(declaration))
        }
    }
}
