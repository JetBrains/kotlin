/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

public sealed external interface JsReference<out T : Any> : JsAny

@Suppress("NOTHING_TO_INLINE")
public inline fun <T : Any> T.toJsReference(): JsReference<T> = unsafeCast<JsReference<T>>()

@Suppress("NOTHING_TO_INLINE")
public inline fun <T : Any> JsReference<T>.get(): T = unsafeCast<T>()