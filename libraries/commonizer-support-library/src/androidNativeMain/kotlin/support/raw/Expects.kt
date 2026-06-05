/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package support.raw

/** Similar to [UIntOrULong] + With Var */
expect class AndroidSizeT

/** Similar to [IntOrLong] + With Var */
expect class AndroidSSizeT

/** Similar to [UIntOrULong] + With Var */
expect value class AndroidStNlink

/** Similar to [SmallUnsignedNumber] + With Var */
expect value class AndroidFExceptT

/** Similar to [SmallSignedNumber] */
expect class AndroidFExceptT_Helper

/** Similar to [SmallUnsignedNumber] + With Var */
expect value class AndroidModeT

/** Similar to [SmallSignedNumber] */
expect class AndroidSModeT_Helper
