/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.resolve.diagnostics

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperInterfaces
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal

// TODO: Implement in K2
object WasmExternalInheritanceChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor is ClassDescriptor && !descriptor.isEffectivelyExternal()) {
            val superClasses = listOfNotNull(descriptor.getSuperClassNotAny()) + descriptor.getSuperInterfaces()
            for (superClass in superClasses) {
                if (superClass.isEffectivelyExternal()) {
                    context.trace.report(
                        ErrorsWasm.NON_EXTERNAL_TYPE_EXTENDS_EXTERNAL_TYPE.on(
                            declaration as KtClassOrObject,
                            superClass.defaultType
                        )
                    )
                }
            }
        }
    }
}
