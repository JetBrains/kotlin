/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.resolve.diagnostics

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal
import org.jetbrains.kotlin.resolve.source.getPsi

object FirWasmJsAssociatedObjectChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor !is ClassDescriptor) return
        if (!descriptor.isEffectivelyExternal()) return

        for (annotationCall in descriptor.annotations) {
            val annotation = annotationCall.annotationClass ?: continue
            if (annotation.annotations.hasAnnotation(StandardClassIds.Annotations.AssociatedObjectKey.asSingleFqName())) {
                context.trace.report(ErrorsWasm.ASSOCIATED_OBJECT_INVALID_BINDING.on(annotationCall.source.getPsi() ?: declaration))
            }
        }
    }
}