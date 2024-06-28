/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.resolve.diagnostics

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperClassifiers
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal

object JsExternalInheritorOnlyChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor is ClassDescriptor && !descriptor.isEffectivelyExternal()) {
            descriptor.getAllSuperClassifiers().forEach { parent ->
                if (parent is ClassDescriptor && AnnotationsUtils.isJsExternalInheritorsOnly(parent)) {
                    context.trace.report(ErrorsJs.JS_EXTERNAL_INHERITORS_ONLY.on(declaration, parent, descriptor))
                }
            }
        }
    }
}
