/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalForeignApi::class)
package kotlinx.cinterop

import kotlin.native.*
import kotlin.native.internal.GCUnsafeCall

@GCUnsafeCall("Kotlin_Interop_createStablePointer")
internal external fun createStablePointer(any: Any): COpaquePointer

@GCUnsafeCall("Kotlin_Interop_disposeStablePointer")
internal external fun disposeStablePointer(pointer: COpaquePointer)

@PublishedApi
@GCUnsafeCall("Kotlin_Interop_derefStablePointer")
internal external fun derefStablePointer(pointer: COpaquePointer): Any
