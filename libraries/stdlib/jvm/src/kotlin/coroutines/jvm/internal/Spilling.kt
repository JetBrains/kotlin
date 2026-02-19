/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.coroutines.jvm.internal

/**
 * This function is called by generated code when visible dead variable is spilled.
 *
 * By default, it returns `null`, but the debugger is expected to replace it with implementation, which returns the argument.
 *
 * This way, we avoid memory leaks, and do not hinder debuggability.
 */
@PublishedApi
@Suppress("UNUSED_PARAMETER", "unused")
internal fun nullOutSpilledVariable(value: Any?): Any? = null