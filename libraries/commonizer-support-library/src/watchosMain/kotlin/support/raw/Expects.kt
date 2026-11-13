/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package support.raw

/**
 * Similar to [UIntOrULong] + With Var.
 * Modeled after [platform.posix.size_t].
 */
expect value class WatchosSizeT

/**
 * Similar to [IntOrLong] + With Var.
 * Modeled after [platform.posix.ssize_t].
 */
expect class WatchosSSizeT

/**
 * Similar to [SmallSignedNumber] + With Var.
 * Modeled after [platform.CoreFoundation.CFBundleRefNum].
 */
expect class WatchosCFBundleRefNum

/**
 * Similar to [FloatOrDouble] + With Var.
 * Modeled after [platform.CoreGraphics.CGFloat].
 */
expect class WatchosCGFloat

/**
 * Similar to [IntOrLong] + With Var.
 * Modeled after [platform.Accelerate.vDSP_Stride].
 */
expect class WatchosPlatformSizeT

/**
 * Similar to [UIntOrULong] + With Var.
 * Modeled after [platform.posix.user_addr_t].
 */
expect value class WatchosPlatformUSizeT
