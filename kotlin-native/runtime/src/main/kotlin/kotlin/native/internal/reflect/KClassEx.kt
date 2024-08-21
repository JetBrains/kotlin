/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalForeignApi::class)

package kotlin.native.internal.reflect

import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.native.internal.GCUnsafeCall
import kotlin.native.internal.InternalForKotlinNative
import kotlin.native.internal.NativePtr
import kotlin.native.internal.escapeAnalysis.Escapes
import kotlin.native.internal.typeInfoPtr
import kotlin.reflect.KClass

/**
 * Get ObjC name of the class if it was bound to [KClass] by ObjC Export or by
 * [@BindClassToObjCName][kotlin.native.internal.objc.BindClassToObjCName] annotation.
 */
@InternalForKotlinNative
public val KClass<*>.objCNameOrNull: String?
    get() {
        val typeInfo = typeInfoPtr
        return if (typeInfo.isNull()) null else objCNameOrNull(typeInfo)
    }

@GCUnsafeCall("Kotlin_native_internal_reflect_objCNameOrNull")
@Escapes.Nothing // The resulting string is always freshly heap-allocated and not stored anywhere.
private external fun objCNameOrNull(typeInfo: NativePtr): String?