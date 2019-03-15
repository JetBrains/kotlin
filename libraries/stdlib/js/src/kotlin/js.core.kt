/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

/**
 * Exposes the JavaScript [undefined property](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/undefined) to Kotlin.
 */
public external val undefined: Nothing?

/**
 * Exposes the JavaScript [eval function](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/eval) to Kotlin.
 */
public external fun eval(expr: String): dynamic
