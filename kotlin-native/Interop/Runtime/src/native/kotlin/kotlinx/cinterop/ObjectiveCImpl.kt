/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("NOTHING_TO_INLINE")
@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package kotlinx.cinterop

import kotlin.native.*
import kotlin.native.internal.*
import kotlin.native.internal.InternalForKotlinNative

@BetaInteropApi
interface ObjCObject
@BetaInteropApi
interface ObjCClass : ObjCObject
@BetaInteropApi
interface ObjCClassOf<T : ObjCObject> : ObjCClass // TODO: T should be added to ObjCClass and all meta-classes instead.
@BetaInteropApi
typealias ObjCObjectMeta = ObjCClass

@BetaInteropApi
interface ObjCProtocol : ObjCObject

@ExportTypeInfo("theForeignObjCObjectTypeInfo")
@OptIn(FreezingIsDeprecated::class)
@kotlin.native.internal.Frozen
internal open class ForeignObjCObject : kotlin.native.internal.ObjCObjectWrapper

@BetaInteropApi
abstract class ObjCObjectBase protected constructor() : ObjCObject {
    @Target(AnnotationTarget.CONSTRUCTOR)
    @Retention(AnnotationRetention.SOURCE)
    annotation class OverrideInit
}

@BetaInteropApi
abstract class ObjCObjectBaseMeta protected constructor() : ObjCObjectBase(), ObjCObjectMeta {}

@BetaInteropApi
fun optional(): Nothing = throw RuntimeException("Do not call me!!!")

@Deprecated(
        "Add @OverrideInit to constructor to make it override Objective-C initializer",
        level = DeprecationLevel.ERROR
)
@TypedIntrinsic(IntrinsicType.OBJC_INIT_BY)
external fun <T : ObjCObjectBase> T.initBy(constructorCall: T): T

@BetaInteropApi
@kotlin.native.internal.ExportForCompiler
private fun ObjCObjectBase.superInitCheck(superInitCallResult: ObjCObject?) {
    if (superInitCallResult == null)
        throw RuntimeException("Super initialization failed")

    if (superInitCallResult.objcPtr() != this.objcPtr())
        throw UnsupportedOperationException("Super initializer has replaced object")
}

internal fun <T : Any?> Any?.uncheckedCast(): T = @Suppress("UNCHECKED_CAST") (this as T)

// Note: if this is called for non-frozen object on a wrong worker, the program will terminate.
@ExperimentalForeignApi
@GCUnsafeCall("Kotlin_Interop_refFromObjC")
external fun <T> interpretObjCPointerOrNull(objcPtr: NativePtr): T?

@ExportForCppRuntime
@ExperimentalForeignApi
inline fun <T : Any> interpretObjCPointer(objcPtr: NativePtr): T = interpretObjCPointerOrNull<T>(objcPtr)!!

@GCUnsafeCall("Kotlin_Interop_refToObjC")
@ExperimentalForeignApi
public external fun Any?.objcPtr(): NativePtr

@GCUnsafeCall("Kotlin_Interop_createKotlinObjectHolder")
@ExperimentalForeignApi
public external fun createKotlinObjectHolder(any: Any?): NativePtr

// Note: if this is called for non-frozen underlying ref on a wrong worker, the program will terminate.
@BetaInteropApi
public inline fun <reified T : Any> unwrapKotlinObjectHolder(holder: Any?): T {
    return unwrapKotlinObjectHolderImpl(holder!!.objcPtr()) as T
}

@PublishedApi
@GCUnsafeCall("Kotlin_Interop_unwrapKotlinObjectHolder")
external internal fun unwrapKotlinObjectHolderImpl(ptr: NativePtr): Any

@ExperimentalForeignApi
class ObjCObjectVar<T>(rawPtr: NativePtr) : CVariable(rawPtr) {
    @Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
    @Suppress("DEPRECATION")
    companion object : CVariable.Type(pointerSize.toLong(), pointerSize)
}

@ExperimentalForeignApi
class ObjCNotImplementedVar<T : Any?>(rawPtr: NativePtr) : CVariable(rawPtr) {
    @Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
    @Suppress("DEPRECATION")
    companion object : CVariable.Type(pointerSize.toLong(), pointerSize)
}

@ExperimentalForeignApi
var <T : Any?> ObjCNotImplementedVar<T>.value: T
    get() = TODO()
    set(_) = TODO()

@ExperimentalForeignApi
typealias ObjCStringVarOf<T> = ObjCNotImplementedVar<T>
@ExperimentalForeignApi
typealias ObjCBlockVar<T> = ObjCNotImplementedVar<T>

@TypedIntrinsic(IntrinsicType.OBJC_CREATE_SUPER_STRUCT)
@PublishedApi
internal external fun createObjCSuperStruct(receiver: NativePtr, superClass: NativePtr): NativePtr

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
@InternalForKotlinNative
public annotation class ExternalObjCClass(val protocolGetter: String = "", val binaryName: String = "")

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.BINARY)
@InternalForKotlinNative
public annotation class ObjCMethod(val selector: String, val encoding: String, val isStret: Boolean = false)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@InternalForKotlinNative
public annotation class ObjCDirect(val symbol: String)

@Target(AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.BINARY)
@InternalForKotlinNative
public annotation class ObjCConstructor(val initSelector: String, val designated: Boolean)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@InternalForKotlinNative
public annotation class ObjCFactory(val selector: String, val encoding: String, val isStret: Boolean = false)

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.BINARY)
@InternalForKotlinNative
public annotation class InteropStubs()

@PublishedApi
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
internal annotation class ObjCMethodImp(val selector: String, val encoding: String)

@PublishedApi
@TypedIntrinsic(IntrinsicType.OBJC_GET_SELECTOR)
internal external fun objCGetSelector(selector: String): COpaquePointer

@kotlin.native.internal.ExportForCompiler
private fun allocObjCObject(clazz: NativePtr): NativePtr {
    val rawResult = objc_allocWithZone(clazz)
    if (rawResult == nativeNullPtr) {
        throw OutOfMemoryError("Unable to allocate Objective-C object")
    }

    // Note: `objc_allocWithZone` returns retained pointer, and thus it must be balanced by the caller.

    return rawResult
}

@TypedIntrinsic(IntrinsicType.OBJC_GET_OBJC_CLASS)
@kotlin.native.internal.ExportForCompiler
private external fun <T : ObjCObject> getObjCClass(): NativePtr

@PublishedApi
@TypedIntrinsic(IntrinsicType.OBJC_GET_MESSENGER)
internal external fun getMessenger(superClass: NativePtr): COpaquePointer?

@PublishedApi
@TypedIntrinsic(IntrinsicType.OBJC_GET_MESSENGER_STRET)
internal external fun getMessengerStret(superClass: NativePtr): COpaquePointer?


internal class ObjCWeakReferenceImpl : kotlin.native.ref.WeakReferenceImpl() {
    @GCUnsafeCall("Konan_ObjCInterop_getWeakReference")
    external override fun get(): Any?
}

@GCUnsafeCall("Konan_ObjCInterop_initWeakReference")
private external fun ObjCWeakReferenceImpl.init(objcPtr: NativePtr)

@kotlin.native.internal.ExportForCppRuntime internal fun makeObjCWeakReferenceImpl(objcPtr: NativePtr): ObjCWeakReferenceImpl {
    val result = ObjCWeakReferenceImpl()
    result.init(objcPtr)
    return result
}

// Konan runtme:

@Deprecated("Use plain Kotlin cast of String to NSString", level = DeprecationLevel.ERROR)
@GCUnsafeCall("Kotlin_Interop_CreateNSStringFromKString")
external fun CreateNSStringFromKString(str: String?): NativePtr

@Deprecated("Use plain Kotlin cast of NSString to String", level = DeprecationLevel.ERROR)
@GCUnsafeCall("Kotlin_Interop_CreateKStringFromNSString")
external fun CreateKStringFromNSString(ptr: NativePtr): String?

@PublishedApi
@GCUnsafeCall("Kotlin_Interop_CreateObjCObjectHolder")
internal external fun createObjCObjectHolder(ptr: NativePtr): Any?

// Objective-C runtime:

@GCUnsafeCall("objc_retainAutoreleaseReturnValue")
@ExperimentalForeignApi
public external fun objc_retainAutoreleaseReturnValue(ptr: NativePtr): NativePtr

@GCUnsafeCall("Kotlin_objc_autoreleasePoolPush")
@ExperimentalForeignApi
public external fun objc_autoreleasePoolPush(): NativePtr

@GCUnsafeCall("Kotlin_objc_autoreleasePoolPop")
@ExperimentalForeignApi
public external fun objc_autoreleasePoolPop(ptr: NativePtr)

@GCUnsafeCall("Kotlin_objc_allocWithZone")
@FilterExceptions
private external fun objc_allocWithZone(clazz: NativePtr): NativePtr

@GCUnsafeCall("Kotlin_objc_retain")
@ExperimentalForeignApi
public external fun objc_retain(ptr: NativePtr): NativePtr

@GCUnsafeCall("Kotlin_objc_release")
@ExperimentalForeignApi
public external fun objc_release(ptr: NativePtr)
