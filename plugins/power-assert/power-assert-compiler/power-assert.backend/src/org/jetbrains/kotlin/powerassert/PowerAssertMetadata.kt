/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert

import org.jetbrains.kotlin.config.LanguageVersion

/**
 * Power-Assert metadata that helps track what version of the compiler was used to generate the
 * `$explained` function overload of the original function. While the version is not currently used
 * for anything, the presence of such metadata indicates that the explained function has been
 * generated. In the future it may indicate what shape the explained function takes, for example,
 * the parameter type of the explanation variable.
 */
@JvmInline
value class PowerAssertMetadata(val data: ByteArray) {
    constructor(version: LanguageVersion) : this(byteArrayOf(version.major.toByte(), version.minor.toByte()))
}
