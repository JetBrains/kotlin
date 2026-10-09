/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:kotlin.internal.JvmBuiltin

package kotlin

import kotlin.internal.RequireKotlin

/**
 * The implicit superclass of all `error class` and `error object` types.
 *
 * This class is part of the experimental Rich Errors feature.
 * See [KEEP-0462](https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0462-rich-errors.md) for more details.
 *
 * To access it, the Rich Errors language feature needs to be enabled using the `-Xrich-errors` compiler argument.
 */
@RequireKotlin("2.5.20", versionKind = COMPILER_VERSION)
@SinceKotlin("2.5")
public abstract class RichError
