/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.hasAnnotation
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.nameOrAnonymous
import org.jetbrains.kotlin.analysis.api.types.KtNonErrorClassType
import org.jetbrains.kotlin.analysis.api.types.KtType
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
context(KtAnalysisSession, KtObjCExportSession)
internal fun KtType.translateToObjCObjectType(): ObjCNonNullReferenceType {
    if (this !is KtNonErrorClassType) return objCErrorType
    val classSymbol = this.symbol as? KtClassOrObjectSymbol ?: return ObjCIdType
    return classSymbol.translateToObjCObjectType()
}

context(KtAnalysisSession, KtObjCExportSession)
private fun KtClassOrObjectSymbol.translateToObjCObjectType(): ObjCNonNullReferenceType {
    if (isObjCMetaClass()) return ObjCMetaClassType
    if (isObjCProtocolClass()) return ObjCClassType("Protocol", extras = objCTypeExtras {
        requiresForwardDeclaration = true
    })

    if (isExternalObjCClass() || isObjCForwardDeclaration()) {
        return if (classKind == KtClassKind.INTERFACE) {
            ObjCProtocolType(nameOrAnonymous.asString().removeSuffix("Protocol"), extras = objCTypeExtras {
                requiresForwardDeclaration = true
            })
        } else {
            ObjCClassType(nameOrAnonymous.asString(), extras = objCTypeExtras {
                requiresForwardDeclaration = true
            })
        }
    }

    return getSuperClassSymbolNotAny()?.translateToObjCObjectType() ?: ObjCIdType
}

context(KtAnalysisSession)
private fun KtClassOrObjectSymbol.isObjCMetaClass(): Boolean {
    if (classId == objCClassClassId) return true
    return getDeclaredSuperInterfaceSymbols().any { superInterfaceSymbol -> superInterfaceSymbol.isObjCMetaClass() }
}

context(KtAnalysisSession)
private fun KtClassOrObjectSymbol.isObjCProtocolClass(): Boolean {
    if (classId == objCProtocolClassId) return true
    return getDeclaredSuperInterfaceSymbols().any { superInterfaceSymbol -> superInterfaceSymbol.isObjCProtocolClass() }
}

context(KtAnalysisSession)
private fun KtClassOrObjectSymbol.isExternalObjCClass(): Boolean {
    return hasAnnotation(NativeStandardInteropNames.externalObjCClassClassId)
}

context(KtAnalysisSession)
private fun KtClassOrObjectSymbol.isObjCForwardDeclaration(): Boolean {
    val classId = classId ?: return false
    return when (NativeForwardDeclarationKind.packageFqNameToKind[classId.packageFqName]) {
        null, NativeForwardDeclarationKind.Struct -> false
        NativeForwardDeclarationKind.ObjCProtocol, NativeForwardDeclarationKind.ObjCClass -> true
    }
}
