/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.internal.PureReifiable

/**
 * Returns an empty array of the specified type [T].
 */
@Suppress("REIFIED_TYPE_PARAMETER_NO_INLINE")
public expect fun <reified @PureReifiable T> emptyArray(): Array<T>
