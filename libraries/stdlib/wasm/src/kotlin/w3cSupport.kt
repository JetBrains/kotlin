/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

@PublishedApi
internal val undefined: Nothing? = null

/**
 * Exposes the JavaScript [eval function](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/eval) to Kotlin.
 */
@JsFun("(s) => eval(s)")
external fun js(code: String): Dynamic