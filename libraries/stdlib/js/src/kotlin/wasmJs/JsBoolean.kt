/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

/** JavaScript primitive boolean */
public actual typealias JsBoolean = Boolean

@Suppress("NOTHING_TO_INLINE")
public actual inline fun JsBoolean.toBoolean(): Boolean = unsafeCast<Boolean>()

@Suppress("NOTHING_TO_INLINE")
public actual inline fun Boolean.toJsBoolean(): JsBoolean = unsafeCast<JsBoolean>()
