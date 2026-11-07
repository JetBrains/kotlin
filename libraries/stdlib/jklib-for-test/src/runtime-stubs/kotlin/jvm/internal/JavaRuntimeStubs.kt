/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal

// Stubs for the declarations of the Java part of the JVM runtime (`libraries/stdlib/jvm/runtime/kotlin/jvm/internal/*.java`)
// that are referenced from the Kotlin sources of the stdlib.

public object Reflection {
    public const val REFLECTION_NOT_AVAILABLE: String = " (Kotlin reflection is not available)"

    @JvmStatic
    public fun getOrCreateKotlinClass(javaClass: Class<*>): kotlin.reflect.KClass<*> = TODO("stub")

    @JvmStatic
    public fun renderLambdaToString(lambda: Lambda<*>): String = TODO("stub")

    @JvmStatic
    public fun renderLambdaToString(lambda: FunctionBase<*>): String = TODO("stub")
}

public object TypeIntrinsics {
    @JvmStatic
    public fun isFunctionOfArity(obj: Any?, arity: Int): Boolean = TODO("stub")
}

public object Ref {
    public class IntRef : java.io.Serializable {
        @JvmField
        public var element: Int = 0
    }
}
