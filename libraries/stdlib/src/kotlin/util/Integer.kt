/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

@SinceKotlin("1.0")
/** Returns the int if it is not `null`, or the 0 int otherwise. */
public fun Int?.orZero(): Int = this ?: 0

@SinceKotlin("1.0")
/** Returns the int if it is not `null` and not zero, or the 0 int otherwise. */
public fun Int?.isNotNullOrZero() = if (this != null && this != 0) this else 0

@SinceKotlin("1.0")
/** Returns function containing int if it is not `null` and not zero, or the 0 int otherwise. */
public fun Int?.isNotNullOrZero(block: (Int) -> Unit) {
    if (this != null && this != 0) block(this)
}