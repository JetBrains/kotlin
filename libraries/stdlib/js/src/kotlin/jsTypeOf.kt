/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

/**
 * Function corresponding to JavaScript's `typeof` operator
 */
@kotlin.internal.InlineOnly
@Suppress("UNUSED_PARAMETER")
public inline fun jsTypeOf(a: Any?): String = js("typeof a")
