/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.objcexport.analysisApiUtils.errorForwardClass
import org.jetbrains.kotlin.objcexport.analysisApiUtils.errorInterface
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getDeclaredSuperInterfaceSymbols
import org.jetbrains.kotlin.objcexport.analysisApiUtils.hasErrorTypes
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.addIfNotNull

context(KtAnalysisSession, KtObjCExportSession)
fun translateToObjCHeader(files: List<KtFile>): ObjCHeader {
    val symbolTranslationQueue = ArrayDeque<KtSymbol>()
    val translatedObjCExportStubs = mutableListOf<ObjCExportStub>()
    val protocolForwardDeclarations = mutableSetOf<String>()
    val classForwardDeclarations = mutableSetOf<ObjCClassForwardDeclaration>()

    val processedClassIds = hashSetOf<ClassId>()

    /* Initially populate the queue with sorted file symbols */
    symbolTranslationQueue.addAll(files.sortedWith(StableFileOrder).map { it.getFileSymbol() })

    fun process(symbol: KtSymbol) {
        when (symbol) {
            /* In this case: Go and translate all classes/objects/interfaces inside the file as well */
            is KtFileSymbol -> {
                val translatedTopLevelFileFacade = symbol.translateToObjCTopLevelInterfaceFileFacade()
                translatedObjCExportStubs.addIfNotNull(translatedTopLevelFileFacade)

                symbolTranslationQueue.addAll(
                    symbol.getFileScope().getClassifierSymbols().sortedWith(StableClassifierOrder)
                )
            }

            /* Translate the Class/Interface, but ensure that all supertypes will be translated also */
            is KtClassOrObjectSymbol -> {

                /* Add the classId to already processed classIds and do not redo if already processed */
                if (!processedClassIds.add(symbol.classIdIfNonLocal ?: return)) {
                    return
                }

                val translatedObjCClassOrProtocol = when (symbol.classKind) {
                    KtClassKind.INTERFACE -> symbol.translateToObjCProtocol()
                    KtClassKind.CLASS -> symbol.translateToObjCClass()
                    KtClassKind.OBJECT -> symbol.translateToObjCObject()
                    else -> return
                } ?: return

                symbol.getDeclaredSuperInterfaceSymbols().forEach { superInterfaceSymbol ->
                    process(superInterfaceSymbol)
                }

                translatedObjCExportStubs.add(translatedObjCClassOrProtocol)
            }
        }
    }

    while (true) {
        val symbol = symbolTranslationQueue.removeFirstOrNull() ?: break
        process(symbol)
    }

    if (translatedObjCExportStubs.hasErrorTypes()) {
        translatedObjCExportStubs.add(errorInterface)
        classForwardDeclarations.add(errorForwardClass)
    }

    protocolForwardDeclarations += translatedObjCExportStubs
        .filterIsInstance<ObjCClass>()
        .flatMap { it.superProtocols }

    classForwardDeclarations += translatedObjCExportStubs
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

    return ObjCHeader(
        stubs = translatedObjCExportStubs.toList(),
        classForwardDeclarations = classForwardDeclarations,
        protocolForwardDeclarations = protocolForwardDeclarations.toSet(),
        additionalImports = emptyList(),
    )
}

context(KtAnalysisSession, KtObjCExportSession)
internal fun KtCallableSymbol.translateToObjCExportStubs(): List<ObjCExportStub> {
    return when (this) {
        is KtConstructorSymbol -> translateToObjCConstructors()
        is KtPropertySymbol -> listOfNotNull(translateToObjCProperty())
        is KtFunctionSymbol -> listOfNotNull(translateToObjCMethod())
        else -> emptyList()
    }
}