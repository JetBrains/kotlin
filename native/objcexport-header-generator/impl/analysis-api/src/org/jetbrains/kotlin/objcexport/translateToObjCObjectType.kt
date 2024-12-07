/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.nameOrAnonymous
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NativeForwardDeclarationKind
import org.jetbrains.kotlin.name.NativeStandardInteropNames
import org.jetbrains.kotlin.name.NativeStandardInteropNames.cInteropPackage
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getDeclaredSuperInterfaceSymbols
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getSuperClassSymbolNotAny
import org.jetbrains.kotlin.objcexport.analysisApiUtils.objCErrorType
import org.jetbrains.kotlin.objcexport.extras.objCTypeExtras
import org.jetbrains.kotlin.objcexport.extras.requiresForwardDeclaration

/**
 * ClassId for 'kotlinx.cinterop.ObjCClass'
 */
private val objCClassClassId = ClassId(cInteropPackage, Name.identifier("ObjCClass"))

/**
 * ClassId for 'kotlinx.cinterop.ObjCProtocol'
 */
private val objCProtocolClassId = ClassId(cInteropPackage, Name.identifier("ObjCProtocol"))


/**
 * Special type translation for types that implement `ObjCObject`:
 * Such types cannot be directly translated but need to use a supertype that is explicitly marked with an
 * `@ExternalObjCClass` annotation. (See [org.jetbrains.kotlin.objcexport.analysisApiUtils.isObjCObjectType]
 */
internal fun ObjCExportContext.translateToObjCObjectType(type: KaType): ObjCNonNullReferenceType {
    if (type !is KaClassType) return objCErrorType
    val classSymbol = type.symbol as? KaClassSymbol ?: return ObjCIdType
    return translateToObjCObjectType(classSymbol)
}

private fun ObjCExportContext.translateToObjCObjectType(symbol: KaClassSymbol): ObjCNonNullReferenceType {
    if (analysisSession.isObjCMetaClass(symbol)) return ObjCMetaClassType
    if (analysisSession.isObjCProtocolClass(symbol)) return ObjCClassType("Protocol", extras = objCTypeExtras {
        requiresForwardDeclaration = true
    })

    if (analysisSession.isExternalObjCClass(symbol) || analysisSession.isObjCForwardDeclaration(symbol)) {
        return if (symbol.classKind == KaClassKind.INTERFACE) {
            ObjCProtocolType(symbol.nameOrAnonymous.asString().removeSuffix("Protocol"), extras = objCTypeExtras {
                requiresForwardDeclaration = true
            })
        } else {
            ObjCClassType(symbol.nameOrAnonymous.asString(), extras = objCTypeExtras {
                requiresForwardDeclaration = true
            })
        }
    }

    val superClassSymbol = analysisSession.getSuperClassSymbolNotAny(symbol)

    return if (superClassSymbol == null) {
        ObjCIdType
    } else {
        translateToObjCObjectType(superClassSymbol)
    }
}

private fun KaSession.isObjCMetaClass(symbol: KaClassSymbol): Boolean {
    if (symbol.classId == objCClassClassId) return true
    return getDeclaredSuperInterfaceSymbols(symbol).any { superInterfaceSymbol -> isObjCMetaClass(superInterfaceSymbol) }
}

private fun KaSession.isObjCProtocolClass(symbol: KaClassSymbol): Boolean {
    if (symbol.classId == objCProtocolClassId) return true
    return getDeclaredSuperInterfaceSymbols(symbol).any { superInterfaceSymbol -> isObjCProtocolClass(superInterfaceSymbol) }
}

private fun KaSession.isExternalObjCClass(symbol: KaClassSymbol): Boolean {
    return NativeStandardInteropNames.externalObjCClassClassId in symbol.annotations
}

private fun KaSession.isObjCForwardDeclaration(symbol: KaClassSymbol): Boolean {
    val classId = symbol.classId ?: return false
    return when (NativeForwardDeclarationKind.packageFqNameToKind[classId.packageFqName]) {
        null, NativeForwardDeclarationKind.Struct -> false
        NativeForwardDeclarationKind.ObjCProtocol, NativeForwardDeclarationKind.ObjCClass -> true
    }
}
