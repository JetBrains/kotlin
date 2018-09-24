/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.internal.InlineOnly


/**
 * Returns a hash code value for the object or zero if the object is `null`.
 *
 * @see Any.hashCode
 */
@SinceKotlin("1.3")
@InlineOnly
public inline fun Any?.hashCode(): Int = this?.hashCode() ?: 0
