/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tooling.core

/**
 * Generic Annotation to mark APIs as 'unsafe'.
 * This annotation is intended for usage outside this module
 */
@RequiresOptIn(level = RequiresOptIn.Level.ERROR, message = "Use safe API counterpart")
annotation class UnsafeApi(val message: String = "")
