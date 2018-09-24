/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.resolve.diagnostics

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.js.naming.NameSuggestion
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker

class JsNameCharsChecker(private val suggestion: NameSuggestion) : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor is PropertyAccessorDescriptor && AnnotationsUtils.getJsName(descriptor) == null) return

        val suggestedName = suggestion.suggest(descriptor) ?: return
        if (suggestedName.stable && suggestedName.names.any { NameSuggestion.sanitizeName(it) != it }) {
            context.trace.report(ErrorsJs.NAME_CONTAINS_ILLEGAL_CHARS.on(declaration))
        }
    }
}
