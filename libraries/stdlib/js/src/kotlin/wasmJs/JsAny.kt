/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlin.js

/**
 * Any JavaScript value except null or undefined
 */
public actual typealias JsAny = Any

@Suppress("NOTHING_TO_INLINE")
public actual inline fun <T : JsAny> JsAny.unsafeCast(): T =
    asDynamic().unsafeCast<T>()
