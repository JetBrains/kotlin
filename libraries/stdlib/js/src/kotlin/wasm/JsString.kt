/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

/** JavaScript primitive string */
public external class JsString internal constructor() : JsAny

@Suppress("NOTHING_TO_INLINE")
public inline fun String.toJsString(): JsString = unsafeCast<JsString>()