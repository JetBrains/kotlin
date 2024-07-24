/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper

/**
 * Represents a Kotlin name with its Viper equivalent.
 *
 * We could directly convert names and pass them around as strings, but this
 * approach makes it easier to see where they came from during debugging.
 */
interface MangledName {
    val mangledType: String?
        get() = null
    val mangledScope: String?
        get() = null
    val mangledBaseName: String
}

val MangledName.mangled: String
    get() = listOfNotNull(mangledType, mangledScope, mangledBaseName).joinToString("$")
