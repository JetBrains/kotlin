/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.util

import org.apache.commons.lang.StringUtils.splitByCharacterTypeCamelCase
import org.jetbrains.kotlin.konan.target.Architecture
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.test.blackbox.support.PackageName

internal val KonanTarget.compressedName: String
    get() = buildString {
        append(family.compressedName)
        name.splitToSequence('_')
            .drop(1)
            .filter { it.none(Char::isDigit) }
            .forEach { append(it[0]) }
        append(architecture.compressedName)
    }

internal val Family.compressedName: Char
    get() = when (this) {
        Family.OSX -> 'o'
        Family.IOS -> 'i'
        Family.TVOS -> 't'
        Family.WATCHOS -> 'w'
        Family.LINUX -> 'l'
        Family.MINGW -> 'm'
        Family.ANDROID -> 'a'
        Family.WASM -> 'j' // because 'w', 'a' and 'm' are already occupied
        Family.ZEPHYR -> 'z'
    }

internal val Architecture.compressedName: String
    get() = when (this) {
        Architecture.X64 -> "x64"
        Architecture.X86 -> "x86"
        Architecture.ARM64 -> "a64"
        Architecture.ARM32 -> "a32"
        Architecture.MIPS32 -> "m32"
        Architecture.MIPSEL32 -> "e32"
        Architecture.WASM32 -> "w32"
    }

internal val Class<*>.compressedSimpleName: String
    get() = splitByCharacterTypeCamelCase(simpleName).joinToString("") { it.take(3) }

internal val PackageName.compressedPackageName: String
    get() {
        val sanitizedName = segments.joinToString("_")
        return if (sanitizedName.length > COMPRESSED_PACKAGE_FQN_MAX_LENGTH) {
            val suffix = "-" + prettyHash(sanitizedName.hashCode())
            sanitizedName.substring(0, COMPRESSED_PACKAGE_FQN_MAX_LENGTH - suffix.length) + suffix
        } else
            sanitizedName
    }

private const val COMPRESSED_PACKAGE_FQN_MAX_LENGTH = 40
