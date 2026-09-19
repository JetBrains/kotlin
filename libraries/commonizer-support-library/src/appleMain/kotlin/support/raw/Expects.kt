/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package support.raw

/**
 * Similar to [UIntOrULong] + With Var.
 * Modeled after [platform.posix.size_t].
 */
expect value class AppleSizeT

/**
 * Similar to [IntOrLong] + With Var.
 * Modeled after [platform.posix.ssize_t].
 */
expect class AppleSSizeT

/**
 * Similar to [FloatOrDouble].
 * Modeled after [platform.CoreGraphics.CGFloat].
 */
expect class AppleCGFloat

/**
 * Similar to [FloatOrDouble].
 * Modeled after [platform.SceneKit.SCNVector3.x].
 */
expect class AppleMatrixComponent

/**
 * Similar to [UIntOrULong] + With Var.
 * Modeled after [platform.posix.ino_t].
 */
expect value class AppleInoT
