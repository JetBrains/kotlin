/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NativeRuntimeNames

@InternalKotlinNativeApi
const val NATIVE_PTR_NAME = "NativePtr"

@InternalKotlinNativeApi
const val NON_NULL_NATIVE_PTR_NAME = "NonNullNativePtr"

@InternalKotlinNativeApi
const val IMMUTABLE_BLOB_OF = "immutableBlobOf"

object KonanFqNames {
    val function = FqName("kotlin.Function")
    val kFunction = FqName("kotlin.reflect.KFunction")
    val packageName = FqName("kotlin.native")
    val internalPackageName = FqName("kotlin.native.internal")
    val nativePtr = internalPackageName.child(Name.identifier(NATIVE_PTR_NAME)).toUnsafe()
    val nonNullNativePtr = internalPackageName.child(Name.identifier(NON_NULL_NATIVE_PTR_NAME)).toUnsafe()
    val Vector128 = FqName("kotlinx.cinterop.Vector128")
    val throws = FqName("kotlin.Throws")
    val cancellationException = FqName("kotlin.coroutines.cancellation.CancellationException")
    val threadLocal = FqName("kotlin.native.concurrent.ThreadLocal")
    val sharedImmutable = FqName("kotlin.native.concurrent.SharedImmutable")
    val volatile = FqName("kotlin.concurrent.Volatile")
    val frozen = FqName("kotlin.native.internal.Frozen")
    val frozenLegacyMM = FqName("kotlin.native.internal.FrozenLegacyMM")
    val leakDetectorCandidate = FqName("kotlin.native.internal.LeakDetectorCandidate")
    val canBePrecreated = FqName("kotlin.native.internal.CanBePrecreated")
    val typedIntrinsic = FqName("kotlin.native.internal.TypedIntrinsic")
    val constantConstructorIntrinsic = FqName("kotlin.native.internal.ConstantConstructorIntrinsic")
    val objCMethod = FqName("kotlinx.cinterop.ObjCMethod")
    val hasFinalizer = FqName("kotlin.native.internal.HasFinalizer")
    val hasFreezeHook = FqName("kotlin.native.internal.HasFreezeHook")
    val gcUnsafeCall = NativeRuntimeNames.Annotations.gcUnsafeCallClassId.asSingleFqName()
    val eagerInitialization = FqName("kotlin.native.EagerInitialization")
    val noReorderFields = FqName("kotlin.native.internal.NoReorderFields")
    val objCName = FqName("kotlin.native.ObjCName")
    val hidesFromObjC = FqName("kotlin.native.HidesFromObjC")
    val refinesInSwift = FqName("kotlin.native.RefinesInSwift")
    val shouldRefineInSwift = FqName("kotlin.native.ShouldRefineInSwift")
    val reflectionPackageName = FqName("kotlin.native.internal.ReflectionPackageName")
}
