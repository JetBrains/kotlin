/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package support.raw

/**
 * Similar to [IntOrLong] + With Var.
 * Modeled after [platform.posix.ssize_t].
 */
expect class LinuxSSizeT

/**
 * Similar to [UIntOrULong] + With Var.
 * Modeled after [platform.posix.size_t].
 */
expect value class LinuxSizeT

/**
 * Similar to [UIntOrULong] + With Var.
 * Modeled after [platform.posix.stat.st_nlink].
 */
expect value class LinuxStNlink

/**
 * Similar to [IntOrLong] + With Var.
 * Modeled after [platform.posix.blksize_t].
 */
expect class LinuxBlksize

/**
 * Similar to [SmallUnsignedNumber] + With Var.
 * Modeled after [platform.posix.fexcept_t].
 */
expect value class LinuxFExceptT

/**
 * Similar to [SmallSignedNumber].
 * Modeled after [platform.posix.__pthread_mutex_s.__spins].
 */
expect class LinuxMutexSpins

/**
 * Similar to [IntOrLong] + With Var.
 * Modeled after [platform.linux.__t_scalar_t].
 */
expect class LinuxScalarT

/**
 * Similar to [UIntOrULong] + With Var.
 * Modeled after [platform.linux.t_uscalar_t].
 */
expect value class LinuxUScalarT
