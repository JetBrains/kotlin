/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.util

import org.jetbrains.kotlin.konan.target.Architecture
import org.jetbrains.kotlin.konan.target.KonanTarget

fun KonanTarget.has32BitPointers(): Boolean = when (this.architecture) {
    Architecture.X86, Architecture.ARM32 -> true
    Architecture.X64, Architecture.ARM64 -> this == KonanTarget.WATCHOS_ARM64
}
