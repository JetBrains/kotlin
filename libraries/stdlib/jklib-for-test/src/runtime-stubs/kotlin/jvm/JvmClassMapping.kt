/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm

import kotlin.reflect.KClass

// Stubs for the declarations of `libraries/stdlib/jvm/runtime/kotlin/jvm/JvmClassMapping.kt` that are needed to compile the
// stdlib and the tests (see `copyStdlibSources` in build.gradle.kts for why the original is not compiled).

@SinceKotlin("2.5")
public val <T> KClass<T & Any>.java: Class<T>
    get() = TODO("stub")

public val <T : Any> KClass<T>.javaPrimitiveType: Class<T>?
    get() = TODO("stub")

public val <T : Any> KClass<T>.javaObjectType: Class<T>
    get() = TODO("stub")

public val <T : Any> Class<T>.kotlin: KClass<T>
    get() = TODO("stub")

public inline val <T : Any> T.javaClass: Class<T>
    get() = TODO("stub")

@Suppress("REIFIED_TYPE_PARAMETER_NO_INLINE")
public fun <reified T : Any> Array<*>.isArrayOf(): Boolean = TODO("stub")
