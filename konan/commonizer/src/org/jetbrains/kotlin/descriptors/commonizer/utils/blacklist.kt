/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.utils

import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.getAbbreviation

private val DEPRECATED_ANNOTATION_FQN = FqName(Deprecated::class.java.name)

internal fun SimpleFunctionDescriptor.isKniBridgeFunction() =
    name.asString().startsWith("kniBridge")

// the following logic determines Kotlin functions with conflicting overloads in Darwin library:
internal fun SimpleFunctionDescriptor.isBlacklistedDarwinFunction(): Boolean {
    if ((containingDeclaration as? PackageFragmentDescriptor)?.fqName?.isUnderDarwinPackage != true)
        return false

    val name = name.asString()
    if (!name.startsWith("simd_") && !name.startsWith("__"))
        return false

    if (annotations.hasAnnotation(DEPRECATED_ANNOTATION_FQN))
        return true

    return valueParameters.any { parameter ->
        val type = parameter.type
        val abbreviationType = type.getAbbreviation()

        abbreviationType != null
                && abbreviationType.declarationDescriptor.name.asString().startsWith("simd_")
                && type.declarationDescriptor.name.asString() == "Vector128"
    }
}
