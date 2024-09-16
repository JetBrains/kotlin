/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir

enum class SirCallableKind {
    FUNCTION,
    INSTANCE_METHOD,
    CLASS_METHOD,
}

val SirCallable.kind: SirCallableKind
    get() = when (this) {
        is SirSetter, is SirGetter -> (parent as SirVariable).kind
        is SirFunction -> (this as SirClassMemberDeclaration).kind
        is SirInit -> SirCallableKind.CLASS_METHOD
    }

val SirClassMemberDeclaration.kind: SirCallableKind
    get() = if (parent is SirModule)
        SirCallableKind.FUNCTION
    else if (isInstance)
        SirCallableKind.INSTANCE_METHOD
    else
        SirCallableKind.CLASS_METHOD