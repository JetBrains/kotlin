package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.symbol

/**
 * Return null when:
 * 1. callable is not top level function or property
 * 2. callable doesn't have extension
 * 3. see other cases at KaType.[getClassIfCategory]
 *
 * In other cases returns extension type:
 * ```kotlin
 * fun Foo.bar() = Unit
 * getClassIfCategory(bar) > Foo
 * ```
 *
 * See K1 [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportMapperKt.getClassIfCategory(org.jetbrains.kotlin.descriptors.CallableMemberDescriptor)]
 */
@Suppress("DEPRECATION")
fun KaSession.getClassIfCategory(symbol: KaCallableSymbol): KaClassSymbol? {
    if (symbol.dispatchReceiverType != null) return null
    return getClassIfCategory(symbol.receiverType)
}

/**
 * Returns null when:
 * 1. type is extension of [kotlinx.cinterop.ObjCObject]
 * 2. type is interface
 * 3. type is inlined
 * 4. type is [isMappedObjCType] == true
 *
 * In other cases returns extension type:
 * ```kotlin
 * fun Foo.bar() = Unit
 * getClassIfCategory(bar) > Foo
 * ```
 *
 * See K1 [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportMapperKt.getClassIfCategory(org.jetbrains.kotlin.types.KotlinType)]
 */
fun KaSession.getClassIfCategory(type: KaType?): KaClassSymbol? {
    if (type == null) return null
    val isInterface = (type.symbol as? KaClassSymbol)?.classKind == KaClassKind.INTERFACE
    val isInline = (type.symbol as? KaNamedClassSymbol)?.isInline
    return if (isInterface == false && isInline == false && type.isAnyType == false && isMappedObjCType(type) == false)
        type.symbol as? KaClassSymbol else null
}