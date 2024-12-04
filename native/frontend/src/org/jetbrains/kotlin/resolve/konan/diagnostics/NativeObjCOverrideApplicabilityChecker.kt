/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.konan.diagnostics

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.objcinterop.getObjCMethodInfo
import org.jetbrains.kotlin.name.NativeStandardInteropNames.Annotations.objCSignatureOverrideClassId
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext


object NativeObjcOverrideApplicabilityChecker : DeclarationChecker {
    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext,
    ) {
        if (descriptor is FunctionDescriptor) {
            val annotation = descriptor.annotations.findAnnotation(objCSignatureOverrideClassId.asSingleFqName()) ?: return
            if (descriptor.getObjCMethodInfo() == null) {
                context.trace.report(ErrorsNative.INAPPLICABLE_OBJC_OVERRIDE.on(DescriptorToSourceUtils.getSourceFromAnnotation(annotation) ?: declaration))
            }
        }
    }
}