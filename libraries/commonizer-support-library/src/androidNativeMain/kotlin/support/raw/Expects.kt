/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package support.raw

/**
 * Similar to [UIntOrULong] + With Var.
 * Modeled after [platform.posix.size_t].
 */
expect value class AndroidSizeT

/**
 * Similar to [IntOrLong] + With Var.
 * Modeled after [platform.posix.ssize_t].
 */
expect class AndroidSSizeT

/**
 * Similar to [UIntOrULong] + With Var.
 * Modeled after [platform.posix.stat.st_nlink].
 */
expect value class AndroidStNlink

/**
 * Similar to [SmallUnsignedNumber] + With Var.
 * Modeled after [platform.posix.fexcept_t].
 */
expect value class AndroidFExceptT

/**
 * Similar to [SmallUnsignedNumber] + With Var.
 * Modeled after [platform.posix.mode_t].
 */
expect value class AndroidModeT
