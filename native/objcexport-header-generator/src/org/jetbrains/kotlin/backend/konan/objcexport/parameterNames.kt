/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.tooling.core.MutableExtras
import org.jetbrains.kotlin.tooling.core.extrasKeyOf

val originParameterNamesKey = extrasKeyOf<List<String>>()

/**
 * Tracks the Kotlin origin associated with [ParameterNames] [ObjCType].
 * Providing the [originParameterNames] can signal a header generation that the class associated with the [ParameterNames] should
 * also be translated for the header.
 */
val ObjCType.originParameterNames: List<String>
    get() {
        return extras[originParameterNamesKey] ?: emptyList()
    }

/**
 * See [ObjCType.originParameterNames]
 */
var MutableExtras.originParameterNames: List<String>
    get() = this[originParameterNamesKey] ?: emptyList()
    set(value) {
        this[originParameterNamesKey] = value
    }