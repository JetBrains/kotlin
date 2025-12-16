/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.hair

enum class HairTODO {
    VIRTUAL_CALLS,
    FAKE_OVERRIDE_CALL,
    STRING_LITERALS,
    EXCEPTIONS,
    ARE_EQUAL_BY_VALUE,
    FLOAT_TRUNCATE,
    VOLATILE,
    CAST,
}

data class HairNotImplementedYet(val what: HairTODO) : Exception(what.toString())

fun notImplemented(what: HairTODO): Nothing = throw HairNotImplementedYet(what)