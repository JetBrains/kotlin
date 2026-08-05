/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package support.raw

import kotlinx.cinterop.*
import kotlinx.cinterop.value as valueFromCinterop

actual typealias NativeSizeT = LinuxSizeT
actual typealias NativeSSizeT = LinuxSSizeT

actual typealias NativeOffT = LinuxSSizeT

actual typealias NativeModeT = UInt

actual typealias NativeInoT = LinuxSizeT

actual typealias NativeSaFamilyT = UShort

actual typealias NativeIntFast32T = LinuxSSizeT
actual typealias NativeUIntFast16T = LinuxSizeT
actual typealias NativeIntFast16T = LinuxSSizeT

actual typealias NativeULong = LinuxSizeT

actual typealias NativeZlibULong = LinuxSizeT

actual typealias NativePidT = Int

actual typealias NativeFExceptT = LinuxFExceptT

actual typealias NativeZCrcT = UInt

actual typealias NativeFsFilCntT = LinuxSizeT

actual typealias NativeStSize = LinuxSSizeT
