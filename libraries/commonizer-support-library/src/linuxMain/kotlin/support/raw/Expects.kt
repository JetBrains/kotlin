/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package support.raw

/** Similar to [IntOrLong] + With Var */
expect class LinuxBlksize

/** Similar to [UIntOrULong] + With Var */
expect value class LinuxUBlksize

/** Similar to [SmallUnsignedNumber] + With Var */
expect value class LinuxFExceptT

/** Similar to [SmallSignedNumber] */
expect class LinuxMutexSpins

/** Similar to [IntOrLong] + With Var */
expect class LinuxScalarT

/** Similar to [UIntOrULong] + With Var */
expect value class LinuxUScalarT
