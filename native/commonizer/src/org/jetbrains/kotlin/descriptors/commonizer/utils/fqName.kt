/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.utils

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.konan.impl.ForwardDeclarationsFqNames

private val STANDARD_KOTLIN_PACKAGE_PREFIXES = listOf(
    KotlinBuiltIns.BUILT_INS_PACKAGE_NAME.asString(),
    "kotlinx"
)

private val KOTLIN_NATIVE_SYNTHETIC_PACKAGES_PREFIXES = ForwardDeclarationsFqNames.syntheticPackages
    .map { fqName ->
        check(!fqName.isRoot)
        fqName.asString()
    }

private const val DARWIN_PACKAGE_PREFIX = "platform.darwin"

internal val FqName.isUnderStandardKotlinPackages: Boolean
    get() = hasAnyPrefix(STANDARD_KOTLIN_PACKAGE_PREFIXES)

internal val FqName.isUnderKotlinNativeSyntheticPackages: Boolean
    get() = hasAnyPrefix(KOTLIN_NATIVE_SYNTHETIC_PACKAGES_PREFIXES)

internal val FqName.isUnderDarwinPackage: Boolean
    get() = asString().hasPrefix(DARWIN_PACKAGE_PREFIX)

private fun FqName.hasAnyPrefix(prefixes: List<String>): Boolean =
    asString().let { fqName -> prefixes.any(fqName::hasPrefix) }

private fun String.hasPrefix(prefix: String): Boolean {
    val lengthDifference = length - prefix.length
    return when {
        lengthDifference == 0 -> this == prefix
        lengthDifference > 0 -> this[prefix.length] == '.' && this.startsWith(prefix)
        else -> false
    }
}
