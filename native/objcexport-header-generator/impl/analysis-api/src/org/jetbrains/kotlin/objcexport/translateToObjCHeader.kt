/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind.*
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.objcexport.analysisApiUtils.errorForwardClass
import org.jetbrains.kotlin.objcexport.analysisApiUtils.errorInterface
import org.jetbrains.kotlin.objcexport.analysisApiUtils.hasErrorTypes
import org.jetbrains.kotlin.psi.KtFile


context(KtAnalysisSession, KtObjCExportSession)
fun translateToObjCHeader(files: List<KtFile>): ObjCHeader {
    val declarations = files.flatMap { ktFile -> ktFile.translateToObjCExportStubs() }.toMutableList()
    val classForwardDeclarations = getClassForwardDeclarations(declarations).toMutableSet()
    val protocolForwardDeclarations = getProtocolForwardDeclarations(declarations)

    if (declarations.hasErrorTypes()) {
        declarations.add(errorInterface)
        classForwardDeclarations.add(errorForwardClass)
    }

    return ObjCHeader(
        stubs = declarations,
        classForwardDeclarations = classForwardDeclarations,
        protocolForwardDeclarations = protocolForwardDeclarations,
        additionalImports = emptyList(),
    )
}

/**
 * Class which have static property must have forward declaration
 *
 * ```
 * @class Foo;
 *
 * @interface Foo
 * @property (class) Foo
 * @end
 * ```
 */
private fun getClassForwardDeclarations(declarations: List<ObjCExportStub>): Set<ObjCClassForwardDeclaration> {
    return declarations
        .filterIsInstance<ObjCClass>()
        .filter { clazz ->
            clazz.members
                .filterIsInstance<ObjCProperty>()
                .any { property ->
                    val className = (property.type as? ObjCClassType)?.className == clazz.name
                    val static = property.propertyAttributes.contains("class")
                    className && static
                }
        }.map { clazz ->
            ObjCClassForwardDeclaration(clazz.name)
        }.toSet()
}

private fun getProtocolForwardDeclarations(declarations: List<ObjCExportStub>) = declarations
    .filterIsInstance<ObjCClass>()
    .flatMap { it.superProtocols }
    .toSet()

context(KtAnalysisSession, KtObjCExportSession)
fun KtFile.translateToObjCExportStubs(): List<ObjCExportStub> {
    return this.getFileSymbol().translateToObjCExportStubs()
}

context(KtAnalysisSession, KtObjCExportSession)
fun KtFileSymbol.translateToObjCExportStubs(): List<ObjCExportStub> {
    return listOfNotNull(translateToObjCTopLevelInterfaceFileFacade()) + getFileScope().getClassifierSymbols()
        .flatMap { classifierSymbol -> classifierSymbol.translateToObjCExportStubs() }
}


context(KtAnalysisSession, KtObjCExportSession)
internal fun KtSymbol.translateToObjCExportStubs(): List<ObjCExportStub> {
    return when {
        this is KtFileSymbol -> translateToObjCExportStubs()
        this is KtClassOrObjectSymbol && classKind == INTERFACE -> listOfNotNull(translateToObjCProtocol())
        this is KtClassOrObjectSymbol && classKind == CLASS -> listOfNotNull(translateToObjCClass())
        this is KtClassOrObjectSymbol && classKind == OBJECT -> listOfNotNull(translateToObjCObject())
        this is KtConstructorSymbol -> translateToObjCConstructors()
        this is KtPropertySymbol -> listOfNotNull(translateToObjCProperty())
        this is KtFunctionSymbol -> listOfNotNull(translateToObjCMethod())
        else -> emptyList()
    }
}