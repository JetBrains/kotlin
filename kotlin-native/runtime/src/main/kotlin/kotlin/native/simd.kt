/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package kotlin.native

import kotlin.native.internal.GCUnsafeCall
import kotlin.native.internal.TypedIntrinsic
import kotlin.native.internal.IntrinsicType
import kotlinx.cinterop.ExperimentalForeignApi


@Deprecated("Use kotlinx.cinterop.Vector128 instead.", ReplaceWith("kotlinx.cinterop.Vector128"))
@DeprecatedSinceKotlin(warningSince = "1.9")
@ExperimentalForeignApi
public typealias Vector128 = kotlinx.cinterop.Vector128

@Suppress("DEPRECATION")
@Deprecated("Use kotlinx.cinterop.vectorOf instead.", ReplaceWith("kotlinx.cinterop.vectorOf(f0, f1, f2, f3)"))
@DeprecatedSinceKotlin(warningSince = "1.9")
@ExperimentalForeignApi
@GCUnsafeCall("Kotlin_Interop_Vector4f_of")
public external fun vectorOf(f0: Float, f1: Float, f2: Float, f3: Float): Vector128

@Suppress("DEPRECATION")
@Deprecated("Use kotlinx.cinterop.vectorOf instead.", ReplaceWith("kotlinx.cinterop.vectorOf(f0, f1, f2, f3)"))
@DeprecatedSinceKotlin(warningSince = "1.9")
@ExperimentalForeignApi
@GCUnsafeCall("Kotlin_Interop_Vector4i32_of")
public external fun vectorOf(f0: Int, f1: Int, f2: Int, f3: Int): Vector128
