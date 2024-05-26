/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalForeignApi::class)
package kotlinx.cinterop

import kotlin.native.*
import kotlin.native.internal.Escapes
import kotlin.native.internal.GCUnsafeCall
import kotlin.native.internal.ExportForCppRuntime
import kotlin.native.internal.PointsTo

@PublishedApi
@GCUnsafeCall("Kotlin_Interop_createStablePointer")
@PointsTo(0x00, 0x01) // ret -> any
internal external fun createStablePointer(any: Any): COpaquePointer

@PublishedApi
@GCUnsafeCall("Kotlin_Interop_disposeStablePointer")
@PointsTo(0x00, 0x00)
internal external fun disposeStablePointer(pointer: COpaquePointer)

@PublishedApi
@GCUnsafeCall("Kotlin_Interop_derefStablePointer")
@PointsTo(0x02, 0x00) // pointer -> ret
internal external fun derefStablePointer(pointer: COpaquePointer): Any

@ExportForCppRuntime
@GCUnsafeCall("Kotlin_Interop_createStackStablePointer")
@PointsTo(0x00, 0x01) // ret -> any
internal external fun createStackStablePointer(any: Any): COpaquePointer

internal class StackStableRef @ExportForCppRuntime constructor(val obj: Any)