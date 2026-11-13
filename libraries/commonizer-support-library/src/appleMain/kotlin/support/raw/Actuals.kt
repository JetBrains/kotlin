/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package support.raw

import kotlinx.cinterop.*
import kotlinx.cinterop.value as valueFromCinterop

actual typealias NativeSizeT = AppleSizeT
actual typealias NativeSSizeT = AppleSSizeT

actual typealias NativeOffT = Long

actual typealias NativeModeT = UShort

actual typealias NativeInoT = AppleInoT

actual typealias NativeSaFamilyT = UByte

actual typealias NativeIntFast32T = Int
actual typealias NativeUIntFast16T = UShort
actual typealias NativeIntFast16T = Short

actual typealias NativeULong = AppleSizeT

actual typealias NativeZlibULong = UInt

actual typealias NativePidT = Int

actual typealias NativeFExceptT = UInt

actual typealias NativeZCrcT = AppleSizeT

actual typealias NativeFsFilCntT = UInt

actual typealias NativeStSize = Long
