/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package support.raw

import kotlinx.cinterop.*
import kotlinx.cinterop.value as valueFromCinterop

actual typealias NativeSizeT = AndroidSizeT
actual typealias NativeSSizeT = AndroidSSizeT

actual typealias NativeOffT = AndroidSSizeT

actual typealias NativeModeT = UInt // wint_t

actual typealias NativeInoT = ULong

actual typealias NativeSaFamilyT = UShort

actual typealias NativeIntFast32T = Int
actual typealias NativeUIntFast16T = UShort
actual typealias NativeIntFast16T = Short

actual typealias NativeULong = AndroidSizeT

actual typealias NativeZlibULong = UInt

actual typealias NativePidT = Int

actual typealias NativeFExceptT = AndroidFExceptT

actual typealias NativeZCrcT = AndroidSizeT

actual typealias NativeFsFilCntT = UInt

actual typealias NativeStSize = Long
