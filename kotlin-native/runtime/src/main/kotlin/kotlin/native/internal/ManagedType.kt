/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("EXPERIMENTAL_API_USAGE_ERROR")

package kotlinx.cinterop

import kotlinx.cinterop.CStructVar
import kotlin.native.internal.Cleaner
import kotlin.native.internal.createCleaner

interface SkiaRefCnt
interface CPlusPlusClass

abstract class ManagedType<T : CStructVar> (val cpp: T)

val <T: CStructVar> ManagedType<T>.ptr: CPointer<T> get() = this.cpp.ptr
