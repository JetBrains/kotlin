/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.konan.diagnostics

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperInterfaces

/**
 * Check that the given class does not inherit from class or implements interface that is
 * marked as HiddenFromObjC (aka "marked with annotation that is marked as HidesFromObjC").
 */
object NativeHiddenFromObjCInheritanceChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor !is ClassDescriptor) return
        // Enum entries inherit from their enum class.
        if (descriptor.kind == ClassKind.ENUM_ENTRY) return
        // Non-public types do not leak to Objective-C API surface, so it is OK for them
        // to inherit from hidden types.
        if (!descriptor.visibility.isPublicAPI) return
        // No need to report anything on class that is hidden itself.
        if (checkClassIsHiddenFromObjC(descriptor)) return

        val isSubtypeOfHiddenFromObjC = descriptor.getSuperInterfaces().any { checkClassIsHiddenFromObjC(it) } ||
                descriptor.getSuperClassNotAny()?.let { checkClassIsHiddenFromObjC(it) } == true
        if (isSubtypeOfHiddenFromObjC) {
            context.trace.report(ErrorsNative.SUBTYPE_OF_HIDDEN_FROM_OBJC.on(declaration))
        }
    }
}

private fun checkContainingClassIsHidden(currentClass: ClassDescriptor): Boolean {
    return (currentClass.containingDeclaration as? ClassDescriptor)?.let {
        if (checkClassIsHiddenFromObjC(it)) {
            true
        } else {
            checkContainingClassIsHidden(it)
        }
    } ?: false
}

private fun checkClassIsHiddenFromObjC(clazz: ClassDescriptor): Boolean {
    clazz.annotations.forEach { annotation ->
        val objcExportMetaAnnotations = annotation.annotationClass?.findObjCExportMetaAnnotations()
            ?: return@forEach
        if (objcExportMetaAnnotations.hidesFromObjCAnnotation != null) {
            return true
        }
    }
    // If outer class is hidden then inner/nested class is hidden as well.
    return checkContainingClassIsHidden(clazz)
}