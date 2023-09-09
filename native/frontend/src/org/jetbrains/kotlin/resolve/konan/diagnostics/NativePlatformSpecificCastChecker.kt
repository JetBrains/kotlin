/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.konan.diagnostics

import org.jetbrains.kotlin.builtins.PlatformSpecificCastChecker
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.konan.getForwardDeclarationKindOrNull
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperClassifiers
import org.jetbrains.kotlin.types.KotlinType

object NativePlatformSpecificCastChecker : PlatformSpecificCastChecker {
    override fun isCastPossible(fromType: KotlinType, toType: KotlinType): Boolean {
        return isCastToAForwardDeclaration(fromType, toType)
    }

    /**
     * Here, we check only that a type has a supertype with a name that could result in a valid cast, to suppress a CAST_NEVER_SUCCEEDS
     * warning. The cast is further checked in NativeForwardDeclarationRttiChecker.
     */
    private fun isCastToAForwardDeclaration(realType: KotlinType, forwardDeclarationType: KotlinType): Boolean {
        val forwardDeclarationClassDescriptor = forwardDeclarationType.constructor.declarationDescriptor
        if (forwardDeclarationClassDescriptor !is ClassDescriptor) return false

        val isACastToAForwardDeclarationType = forwardDeclarationClassDescriptor.getForwardDeclarationKindOrNull() != null
        if (!isACastToAForwardDeclarationType) return false

        val forwardDeclarationClassName = forwardDeclarationClassDescriptor.classId?.relativeClassName ?: return false
        val isAnySuperClassifierNamedAsTheForwardDeclaration = realType.constructor.declarationDescriptor
            ?.getAllSuperClassifiers()?.any { superClassifier ->
                superClassifier.classId?.relativeClassName == forwardDeclarationClassName
            } ?: false

        return isAnySuperClassifierNamedAsTheForwardDeclaration
    }

}