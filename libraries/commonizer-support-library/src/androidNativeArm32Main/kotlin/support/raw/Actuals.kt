/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package support.raw

import kotlinx.cinterop.*
import kotlinx.cinterop.value as valueFromCinterop

actual typealias AndroidSizeT = UInt
actual typealias AndroidSSizeT = Int

actual typealias AndroidStNlink = UInt

actual typealias AndroidFExceptT = UInt
actual typealias AndroidFExceptT_Helper = Int

actual typealias AndroidModeT = UShort
actual typealias AndroidSModeT_Helper = Short
