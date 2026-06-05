/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package support.raw

import kotlinx.cinterop.*
import kotlinx.cinterop.value as valueFromCinterop

actual typealias NativeSizeT = ULong
actual typealias NativeSSizeT = Long

actual typealias NativeOffT = Long

actual typealias NativeModeT = UInt
actual typealias NativeSModeT_Helper = Int

actual typealias NativeInoT = ULong
actual typealias NativeInoT_ReturnType = ULong
actual typealias NativeSInoT_Helper = Long

actual typealias NativeSaFamilyT = UShort
actual typealias NativeSSaFamilyT_Helper = Short

actual typealias NativeDispatchQueuePriorityT = Long

actual typealias NativeIntFast32T = Long
actual typealias NativeUIntFast16T = ULong
actual typealias NativeIntFast16T = Long

actual typealias NativeULong = ULong
actual typealias NativeLong_Helper = Long

actual typealias NativeZlibULong = ULong

actual typealias NativePidT = Int

actual typealias NativeFExceptT = LinuxFExceptT
actual typealias NativeFExceptT_Helper = LinuxMutexSpins

actual typealias NativeZCrcT = UInt
actual typealias NativeSZCrcT = Int

actual typealias NativeFsFilCntT = ULong

actual typealias NativeThOff = UByte
