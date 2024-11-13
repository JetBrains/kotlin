/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotated
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.name.NativeStandardInteropNames
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getAllContainingDeclarations

/**
 * Certain set of classifiers is visible, but should be replaced with interface/protocol with attribute `unavailable`
 * ```objective-c
 * __attribute__((unavailable("can't be imported")))
 * @interface Foo
 * @end
 * ```
 *
 * Classifier is unavailable when:
 * - Super type is mapped type, for example [List], any [NSNumberKind]
 * - Super type is [NativeStandardInteropNames.objCObjectClassId] and annotated with [NativeStandardInteropNames.externalObjCClassClassId]
 */
internal fun KaSession.isUnavailableObjCClassifier(symbol: KaClassSymbol): Boolean {
    return isSuperTypeMapped(symbol) || isObjCClass(symbol)
}

internal fun KaSession.unavailableObjCProtocol(
    name: String,
    symbol: KaClassSymbol,
): ObjCProtocol {
    return ObjCProtocolImpl(
        name = name,
        origin = null,
        attributes = buildUnavailableAttribute(symbol),
        comment = null,
        superProtocols = emptyList(),
        members = emptyList(),
    )
}

internal fun KaSession.unavailableObjCInterface(
    name: String,
    symbol: KaClassSymbol,
): ObjCInterface {
    return ObjCInterfaceImpl(
        name = name,
        comment = null,
        origin = null,
        attributes = buildUnavailableAttribute(symbol),
        superProtocols = emptyList(),
        members = emptyList(),
        categoryName = null,
        generics = emptyList(),
        superClass = "NSObject",
        superClassGenerics = emptyList()
    )
}

internal fun KaSession.isSuperTypeMapped(symbol: KaClassSymbol?): Boolean {
    symbol?.superTypes?.forEach { type ->
        if (isMappedObjCType(type) || isSuperTypeMapped(type.symbol as? KaClassSymbol)) return true
    }
    return false
}

/**
 * K1 [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.attributesForUnexposed]
 */
private fun KaSession.buildUnavailableAttribute(descriptor: KaClassSymbol): List<String> {
    val message = when {
        isKotlinObjCClass(descriptor) -> "Kotlin subclass of Objective-C class "
        else -> ""
    } + "can't be imported"
    return listOf("unavailable(\"$message\")")
}

/**
 * [org.jetbrains.kotlin.ir.objcinterop.ObjCInteropKt.isKotlinObjCClass]
 */
internal fun KaSession.isKotlinObjCClass(symbol: KaClassSymbol): Boolean {
    return isObjCClass(symbol) && !isAnnotatedAsExternalObjCClass(symbol)
}

internal fun isObjCClass(symbol: KaClassSymbol): Boolean {
    return symbol.classId?.packageFqName != interopPackageName &&
        symbol.superTypes.any { it.symbol?.classId?.asSingleFqName() == objCObjectFqName }
}

internal fun KaSession.isAnnotatedAsExternalObjCClass(symbol: KaClassSymbol): Boolean {
    return symbol.isAnnotatedAsExternalObjCClass || getAllContainingDeclarations(symbol).filterIsInstance<KaClassSymbol>().any {
        it.isAnnotatedAsExternalObjCClass
    }
}

internal val KaAnnotated.isAnnotatedAsExternalObjCClass: Boolean
    get() {
        return annotations.any { annotation -> annotation.classId?.asSingleFqName() == externalObjCClassFqName }
    }

private val interopPackageName = NativeStandardInteropNames.cInteropPackage
private val objCObjectFqName = NativeStandardInteropNames.objCObjectClassId.asSingleFqName()
private val externalObjCClassFqName = NativeStandardInteropNames.externalObjCClassClassId.asSingleFqName()