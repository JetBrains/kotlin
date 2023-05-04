/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(ExperimentalForeignApi::class)

package kotlin.native.internal

import kotlin.reflect.KClass
import kotlinx.cinterop.*

@ExportForCompiler
internal class KClassImpl<T : Any>(private val typeInfo: NativePtr) : KClass<T> {

    @ExportForCompiler
    @ConstantConstructorIntrinsic("KCLASS_IMPL")
    @Suppress("UNREACHABLE_CODE")
    constructor() : this(TODO("This is intrinsic constructor and it shouldn't be used directly"))

    // TODO: consider replacing '$' by another delimeter that can't be used in class name specified with backticks (``)
    override val simpleName: String?
        get() = getRelativeName(typeInfo, true)?.substringAfterLast('.')?.substringAfterLast('$')

    override val qualifiedName: String?
        get() {
            val packageName = getPackageName(typeInfo, true) ?: return null
            val relativeName = getRelativeName(typeInfo, true) ?: return null
            return if (packageName.isEmpty()) relativeName else "$packageName.$relativeName"
        }

    override fun isInstance(value: Any?): Boolean = value != null && isInstance(value, this.typeInfo)

    override fun equals(other: Any?): Boolean =
            other is KClassImpl<*> && this.typeInfo == other.typeInfo

    override fun hashCode(): Int = typeInfo.hashCode()

    override fun toString(): String = "class ${fullName ?: "<anonymous>"}"

    internal val fullName: String?
        get() {
            val relativeName = getRelativeName(typeInfo, false) ?: return null
            val packageName: String? = getPackageName(typeInfo, false)
            return if (packageName?.isEmpty() ?: true) relativeName else "$packageName.$relativeName"
        }

    internal fun findAssociatedObjectImpl(key: KClassImpl<*>): Any? =
            findAssociatedObjectImpl(this.typeInfo, key.typeInfo)
}

internal val KClass<*>.fullName: String?
    get() = (this as? KClassImpl<*>)?.fullName

@PublishedApi
internal fun KClass<*>.findAssociatedObject(key: KClass<*>): Any? =
        if (this is KClassImpl<*> && key is KClassImpl<*>) {
            this.findAssociatedObjectImpl(key)
        } else {
            null
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
private external fun findAssociatedObjectImpl(typeInfo: NativePtr, key: NativePtr): Any?

@ExportForCompiler
@GCUnsafeCall("Kotlin_Any_getTypeInfo")
internal external fun getObjectTypeInfo(obj: Any): NativePtr

@GCUnsafeCall("Kotlin_TypeInfo_getPackageName")
private external fun getPackageName(typeInfo: NativePtr, checkFlags: Boolean): String?

@GCUnsafeCall("Kotlin_TypeInfo_getRelativeName")
private external fun getRelativeName(typeInfo: NativePtr, checkFlags: Boolean): String?

@GCUnsafeCall("Kotlin_TypeInfo_isInstance")
private external fun isInstance(obj: Any, typeInfo: NativePtr): Boolean
