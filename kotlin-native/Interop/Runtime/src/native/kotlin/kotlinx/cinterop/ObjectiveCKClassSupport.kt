/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlinx.cinterop

import kotlin.native.internal.KClassImpl
import kotlin.reflect.KClass

/**
 * If [objCClass] is a class generated to Objective-C header for Kotlin class,
 * returns [KClass] for that original Kotlin class.
 *
 * Otherwise returns `null`.
 */
fun getOriginalKotlinClass(objCClass: ObjCClass): KClass<*>? {
    val typeInfo = getTypeInfoForClass(objCClass.objcPtr())
    if (typeInfo.isNull()) return null

    return KClassImpl<Any>(typeInfo)
}

/**
 * If [objCProtocol] is a protocol generated to Objective-C header for Kotlin class,
 * returns [KClass] for that original Kotlin class.
 *
 * Otherwise returns `null`.
 */
fun getOriginalKotlinClass(objCProtocol: ObjCProtocol): KClass<*>? {
    val typeInfo = getTypeInfoForProtocol(objCProtocol.objcPtr())
    if (typeInfo.isNull()) return null

    return KClassImpl<Any>(typeInfo)
}

@SymbolName("Kotlin_ObjCInterop_getTypeInfoForClass")
private external fun getTypeInfoForClass(ptr: NativePtr): NativePtr

@SymbolName("Kotlin_ObjCInterop_getTypeInfoForProtocol")
private external fun getTypeInfoForProtocol(ptr: NativePtr): NativePtr