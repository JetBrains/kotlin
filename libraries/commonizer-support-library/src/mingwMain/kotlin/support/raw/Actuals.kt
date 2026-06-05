/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package support.raw

import kotlinx.cinterop.*
import kotlinx.cinterop.value as valueFromCinterop

actual typealias NativeSizeT = ULong
actual typealias NativeSSizeT = Long

actual typealias NativeOffT = Int

actual typealias NativeModeT = UShort
actual typealias NativeSModeT_Helper = Short

actual typealias NativeInoT = UShort
actual typealias NativeInoT_ReturnType = UInt
actual typealias NativeSInoT_Helper = Short

actual typealias NativeSaFamilyT = UShort
actual typealias NativeSSaFamilyT_Helper = Short

actual typealias NativeDispatchQueuePriorityT = Int

actual typealias NativeIntFast32T = Int
actual typealias NativeUIntFast16T = UShort
actual typealias NativeIntFast16T = Short

actual typealias NativeULong = UInt
actual typealias NativeLong_Helper = Int

actual typealias NativeZlibULong = UInt

actual typealias NativePidT = Long

actual typealias NativeFExceptT = UShort
actual typealias NativeFExceptT_Helper = Short

actual typealias NativeZCrcT = UInt
actual typealias NativeSZCrcT = Int

actual typealias NativeFsFilCntT = UInt

actual typealias NativeThOff = UInt
