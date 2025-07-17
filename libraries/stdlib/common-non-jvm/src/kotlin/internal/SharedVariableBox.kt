/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.internal

// NOTE: It is deliberate that only the generic variant is marked as @PublishedAbi.
// This is to minimize the KLIB ABI surface.
// On the first compilation stage we only use the generic class,
// and on the second stage we replace SharedVariableBox<Int> with SharedVariableBoxInt where possible as an optimization,
// see SharedVariablesPrimitiveBoxSpecializationLowering.

@PublishedApi
@UsedFromCompilerGeneratedCode
internal class SharedVariableBox<T>(var element: T)

@UsedFromCompilerGeneratedCode
internal class SharedVariableBoxBoolean(var element: Boolean)

@UsedFromCompilerGeneratedCode
internal class SharedVariableBoxByte(var element: Byte)

@UsedFromCompilerGeneratedCode
internal class SharedVariableBoxShort(var element: Short)

@UsedFromCompilerGeneratedCode
internal class SharedVariableBoxInt(var element: Int)

@UsedFromCompilerGeneratedCode
internal class SharedVariableBoxLong(var element: Long)

@UsedFromCompilerGeneratedCode
internal class SharedVariableBoxFloat(var element: Float)

@UsedFromCompilerGeneratedCode
internal class SharedVariableBoxDouble(var element: Double)

@UsedFromCompilerGeneratedCode
internal class SharedVariableBoxChar(var element: Char)
