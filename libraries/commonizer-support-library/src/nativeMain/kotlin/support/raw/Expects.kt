/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package support.raw

/** Similar to [IntOrLong] + With Var */
expect class NativeSSizeT

/** Similar to [UIntOrULong] + With Var */
expect value class NativeSizeT

/** Similar to [IntOrLong] + With Var */
expect class NativeOffT

/** Similar to [SmallUnsignedNumber] + With Var */
expect value class NativeModeT

/** Similar to [SmallSignedNumber] */
expect class NativeSModeT_Helper

/** Similar to [UShortOrULong] + With Var */
expect value class NativeInoT

/** Similar to [UIntOrULong] */
expect value class NativeInoT_ReturnType

/** Similar to [ShortOrLong] */
expect class NativeSInoT_Helper

/** Similar to [SmallUnsignedNumber] + With Var */
expect value class NativeSaFamilyT

/** Similar to [SmallSignedNumber] */
expect class NativeSSaFamilyT_Helper

/** Similar to [IntOrLong] */
expect class NativeDispatchQueuePriorityT

/** Similar to [IntOrLong] + With Var */
expect class NativeIntFast32T

/** Similar to [UShortOrULong] + With Var */
expect value class NativeUIntFast16T

/** Similar to [ShortOrLong] + With Var */
expect class NativeIntFast16T

/** Similar to [UIntOrULong] + With Var */
expect value class NativeULong

/** Similar to [IntOrLong] */
expect class NativeLong_Helper

/** Similar to [UIntOrULong] */
expect value class NativeZlibULong

/** Similar to [IntOrLong] + With Var */
expect class NativePidT

/** Similar to [SmallUnsignedNumber] + With Var */
expect value class NativeFExceptT

/** Similar to [SmallSignedNumber] */
expect class NativeFExceptT_Helper

/** Similar to [UIntOrULong] + With Var */
expect value class NativeZCrcT

/** Similar to [IntOrLong] */
expect class NativeSZCrcT

/** Similar to [UIntOrULong] + With Var */
expect value class NativeFsFilCntT

/** Similar to [SmallUnsignedNumber] */
expect value class NativeThOff
