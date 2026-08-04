/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package support.raw

/**
 * Similar to [IntOrLong] + With Var.
 * Modeled after [platform.posix.ssize_t].
 */
expect class NativeSSizeT

/**
 * Similar to [UIntOrULong] + With Var.
 * Modeled after [platform.posix.size_t].
 */
expect value class NativeSizeT

/**
 * Similar to [IntOrLong] + With Var.
 * Modeled after [platform.posix.off_t].
 */
expect class NativeOffT

/**
 * Similar to [SmallUnsignedNumber] + With Var.
 * Modeled after [platform.posix.mode_t].
 */
expect value class NativeModeT

/** Similar to [SmallSignedNumber] */
expect class NativeSModeT_Helper

/**
 * Similar to [UShortOrULong] + With Var.
 * Modeled after [platform.posix.ino_t].
 */
expect value class NativeInoT

/** Similar to [UIntOrULong] */
expect value class NativeInoT_ReturnType

/** Similar to [ShortOrLong] */
expect class NativeSInoT_Helper

/**
 * Similar to [SmallUnsignedNumber] + With Var.
 * Modeled after [platform.posix.sa_family_t] (absent on MinGW).
 */
expect value class NativeSaFamilyT

/** Similar to [SmallSignedNumber] */
expect class NativeSSaFamilyT_Helper

/**
 * Similar to [IntOrLong].
 * Modeled after [platform.darwin.dispatch_queue_priority_t].
 */
expect class NativeDispatchQueuePriorityT

/**
 * Similar to [IntOrLong] + With Var.
 * Modeled after [platform.posix.int_fast32_t].
 */
expect class NativeIntFast32T

/**
 * Similar to [UShortOrULong] + With Var.
 * Modeled after [platform.posix.uint_fast16_t].
 */
expect value class NativeUIntFast16T

/**
 * Similar to [ShortOrLong] + With Var.
 * Modeled after [platform.posix.int_fast16_t].
 */
expect class NativeIntFast16T

/**
 * Similar to [UIntOrULong] + With Var.
 * Modeled after [platform.posix.u_long].
 */
expect value class NativeULong

/** Similar to [IntOrLong] */
expect class NativeLong_Helper

/**
 * Similar to [UIntOrULong].
 * Modeled after [platform.zlib.uLong].
 */
expect value class NativeZlibULong

/**
 * Similar to [IntOrLong] + With Var.
 * Modeled after [platform.posix.pid_t].
 */
expect class NativePidT

/**
 * Similar to [SmallUnsignedNumber] + With Var.
 * Modeled after [platform.posix.fexcept_t].
 */
expect value class NativeFExceptT

/** Similar to [SmallSignedNumber] */
expect class NativeFExceptT_Helper

/**
 * Similar to [UIntOrULong] + With Var.
 * Modeled after [platform.zlib.z_crc_t].
 */
expect value class NativeZCrcT

/** Similar to [IntOrLong] */
expect class NativeSZCrcT

/**
 * Similar to [UIntOrULong] + With Var.
 * Modeled after [platform.posix.fsfilcnt_t] (absent on MinGW).
 */
expect value class NativeFsFilCntT

/** Similar to [SmallUnsignedNumber] */
expect value class NativeThOff
