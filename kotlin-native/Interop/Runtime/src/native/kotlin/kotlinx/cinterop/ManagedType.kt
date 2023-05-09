/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.cinterop

import kotlinx.cinterop.CStructVar

@ExperimentalForeignApi
interface SkiaRefCnt

@ExperimentalForeignApi
interface CPlusPlusClass

@ExperimentalForeignApi
abstract class ManagedType<T : CStructVar>(val cpp: T)

@ExperimentalForeignApi
val <T : CStructVar> ManagedType<T>.ptr: CPointer<T> get() = this.cpp.ptr
