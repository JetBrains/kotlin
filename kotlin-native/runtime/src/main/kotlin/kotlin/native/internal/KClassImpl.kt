/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(ExperimentalForeignApi::class)

package kotlin.native.internal

import kotlin.reflect.KClass
import kotlinx.cinterop.*
import kotlin.native.internal.escapeAnalysis.Escapes

@ExportForCompiler
internal class KClassImpl<T : Any>(override val typeInfo: NativePtr) : KClass<T>, TypeInfoHolder {

    @ExportForCompiler
    @ConstantConstructorIntrinsic("KCLASS_IMPL")
    @Suppress("UNREACHABLE_CODE")
    constructor() : this(TODO("This is intrinsic constructor and it shouldn't be used directly"))

    override val simpleName: String?
        get() = TypeInfoNames(typeInfo).simpleName

    override val qualifiedName: String?
        get() = TypeInfoNames(typeInfo).qualifiedName

    override fun isInstance(value: Any?): Boolean = value != null && isInstance(value, this.typeInfo)

    override fun equals(other: Any?): Boolean =
            other is KClass<*> && this.typeInfo == other.typeInfoPtr

    override fun hashCode(): Int = typeInfo.hashCode()

    override fun toString(): String = "class ${fullName ?: "<anonymous>"}"
}

internal val KClass<*>.typeInfoPtr: NativePtr
    get() = (this as? TypeInfoHolder)?.typeInfo ?: NativePtr.NULL

internal val KClass<*>.fullName: String?
    get() = typeInfoPtr.takeUnless { it.isNull() }?.let { TypeInfoNames(it).fullName }

@PublishedApi
internal fun KClass<*>.findAssociatedObject(keyClass: KClass<*>): Any? {
    val typeInfo = this.typeInfoPtr.takeUnless { it.isNull() } ?: return null
    val key = keyClass.typeInfoPtr.takeUnless { it.isNull() } ?: return null
    return findAssociatedObjectImpl(typeInfo, key)
}

@PublishedApi
internal class KClassUnsupportedImpl(private val message: String) : KClass<Any> {
    override val simpleName: String? get() = error(message)

    override val qualifiedName: String? get() = error(message)

    override fun isInstance(value: Any?): Boolean = error(message)

    override fun equals(other: Any?): Boolean = error(message)

    override fun hashCode(): Int = error(message)

    override fun toString(): String = "unreflected class ($message)"
}

@GCUnsafeCall("Kotlin_TypeInfo_findAssociatedObject")
@Escapes(0b100) // The return value is stored in a global.
private external fun findAssociatedObjectImpl(typeInfo: NativePtr, key: NativePtr): Any?

@ExportForCompiler
@GCUnsafeCall("Kotlin_Any_getTypeInfo")
@Escapes.Nothing
internal external fun getObjectTypeInfo(obj: Any): NativePtr


@GCUnsafeCall("Kotlin_TypeInfo_isInstance")
@Escapes.Nothing
internal external fun isInstance(obj: Any, typeInfo: NativePtr): Boolean

@ExportForCompiler
@TransparentForDebugger
internal fun <T : Any> checkNotNull(obj: T?): T {
    if (obj == null)
        throw NullPointerException()
    return obj
}

@ExportForCompiler
@TransparentForDebugger
internal fun <T : Any> downcast(obj: T?, typeInfo: NativePtr, nullable: Boolean): T? {
    if (obj == null) {
        if (nullable) return obj
        throw NullPointerException()
    }
    if (!isInstance(obj, typeInfo))
        ThrowClassCastException(obj, typeInfo)
    return obj
}
