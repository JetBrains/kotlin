/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package support.raw

import kotlinx.cinterop.*
import kotlinx.cinterop.value as valueFromCinterop

actual typealias AndroidSizeT = ULong
actual typealias AndroidSSizeT = Long

actual typealias AndroidStNlink = ULong

actual typealias AndroidFExceptT = UInt
actual typealias AndroidFExceptT_Helper = Int

actual typealias AndroidModeT = UInt
actual typealias AndroidSModeT_Helper = Int
