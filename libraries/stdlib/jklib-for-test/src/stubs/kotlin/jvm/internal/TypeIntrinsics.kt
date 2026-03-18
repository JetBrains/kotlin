/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlin.jvm.internal

import kotlin.Function

// Stubbed to provide essential runtime type checking and casting bridges required by the compiler backend,
// without including the full kotlin.jvm.internal package.
object TypeIntrinsics {
    @JvmStatic
    fun isFunctionOfArity(obj: Any?, arity: Int): Boolean {
        return obj is Function<*> && (obj as FunctionBase<*>).arity == arity
    }

    @JvmStatic
    fun throwCce(obj: Any?, message: String) {
        throw ClassCastException(message)
    }

    @JvmStatic
    fun throwCce(message: String) {
        throw ClassCastException(message)
    }
}
