/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:kotlin.internal.JvmBuiltin

package kotlin

import kotlin.internal.RequireKotlin
import kotlin.internal.RequireKotlinVersionKind

/**
 * The superclass of all `error class` and `error object` types.
 */
@RequireKotlin("2.5", versionKind = RequireKotlinVersionKind.COMPILER_VERSION)
@SinceKotlin("2.5")
public abstract class RichError

