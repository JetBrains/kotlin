/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package support.raw

import kotlinx.cinterop.*
import kotlinx.cinterop.value as valueFromCinterop

actual typealias LinuxMutexSpins = Int
actual typealias LinuxFExceptT = UInt

actual typealias LinuxScalarT = Long
actual typealias LinuxUScalarT = ULong

actual typealias LinuxUBlksize = UInt
actual typealias LinuxBlksize = Int
