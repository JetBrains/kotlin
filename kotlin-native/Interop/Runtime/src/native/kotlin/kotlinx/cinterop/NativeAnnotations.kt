/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.cinterop

/**
 * Instructs the compiler to allocate a raw [kotlinx.cinterop.CPointer] directly on stack.
 * @param size elements count of the array (not the size in bytes).
 * @param clear should the array be zeroed out or not.
 */
@ExperimentalForeignApi
@Target(AnnotationTarget.LOCAL_VARIABLE)
public annotation class StackAlloc(val size: Int, val clear: Boolean = true)
