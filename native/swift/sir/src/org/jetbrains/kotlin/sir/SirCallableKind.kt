/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir

enum class SirCallableKind {
    FUNCTION,
    INSTANCE_METHOD,
    CLASS_METHOD,
    STATIC_METHOD,
}

val SirVariable.kind: SirCallableKind
    get() = getter.kind
