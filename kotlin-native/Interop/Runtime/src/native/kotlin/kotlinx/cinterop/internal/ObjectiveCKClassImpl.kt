/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalForeignApi::class)
package kotlinx.cinterop.internal

import kotlin.native.internal.*
import kotlin.reflect.KClass
import kotlinx.cinterop.*

@ExportForCompiler
internal class ObjectiveCKClassImpl<T : Any>(private val objcClassPtr: kotlin.native.internal.NativePtr) : KClass<T>, TypeInfoHolder {
    @ExportForCompiler
    @ConstantConstructorIntrinsic("OBJC_KCLASS_IMPL")
    @Suppress("UNREACHABLE_CODE")
    constructor() : this(TODO("This is intrinsic constructor and it shouldn't be used directly"))

    // Consider caching the value
    override val typeInfo: kotlin.native.internal.NativePtr
        get() = getTypeInfoForObjCClassPtr(objcClassPtr)

    override val simpleName: String?
        get() = TypeInfoNames(typeInfo).simpleName

    override val qualifiedName: String?
        get() = TypeInfoNames(typeInfo).qualifiedName

    override fun isInstance(value: Any?): Boolean = value != null && isInstance(value, typeInfo)

    override fun equals(other: Any?): Boolean =
            other is KClass<*> && typeInfo == other.typeInfoPtr

    override fun hashCode(): Int = typeInfo.hashCode()

    override fun toString(): String = "class ${fullName ?: "<anonymous>"}"
}

@GCUnsafeCall("Kotlin_ObjCInterop_getTypeInfoForObjCClassPtr")
private external fun getTypeInfoForObjCClassPtr(objcClassPtr: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr