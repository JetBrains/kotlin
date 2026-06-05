/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package support.raw

import kotlinx.cinterop.*
import kotlinx.cinterop.value as valueFromCinterop

actual typealias AppleSizeT = ULong
actual typealias AppleSSizeT = Long

actual typealias AppleMatrixComponent = Double

actual typealias AppleCGFloat = Double

actual typealias AppleInoT = MacosInoT
//actual --typealias AppleInoT_ReturnType = MacosInoT
actual typealias AppleSInoT_Helper = MacosSInoT_Helper

actual typealias AppleUInt16T = MacosUInt16T
