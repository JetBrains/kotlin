/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.objcexport.analysisApiUtils.*
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.addIfNotNull

context(KtAnalysisSession, KtObjCExportSession)
fun translateToObjCHeader(files: List<KtFile>): ObjCHeader {
    val stubs = mutableListOf<ObjCExportStub>()
    val protocolForwardDeclarations = mutableSetOf<String>()
    val classForwardDeclarations = mutableSetOf<ObjCClassForwardDeclaration>()

    val symbolTranslationQueue = mutableListOf<KtSymbol>()
    val translatedClassifiers = mutableMapOf<ClassId, ObjCClass>()

    fun process(
        symbol: KtSymbol,
        forwardProtocolsAndClasses: Boolean = false,
    ): List<ObjCExportStub> {
        val result = mutableListOf<ObjCExportStub>()
        when (symbol) {
            /* In this case: Go and translate all classes/objects/interfaces inside the file as well */
            is KtFileSymbol -> {
                val translatedTopLevelFileFacade = symbol.translateToObjCTopLevelInterfaceFileFacade()
                result.addIfNotNull(translatedTopLevelFileFacade)

                symbolTranslationQueue.addAll(
                    symbol.getAllClassOrObjectSymbols().sortedWith(StableClassifierOrder)
                )
            }

            /* Translate the Class/Interface, but ensure that all supertypes will be translated also */
            is KtClassOrObjectSymbol -> {
                val classId = symbol.classIdIfNonLocal ?: return result

                /* Add the classId to already processed classIds and do not redo if already processed */
                val stub = translatedClassifiers.getOrPut(classId) {
                    val translatedObjCClassOrProtocol = when (symbol.classKind) {
                        KtClassKind.INTERFACE -> symbol.translateToObjCProtocol()
                        KtClassKind.CLASS -> symbol.translateToObjCClass()
                        KtClassKind.OBJECT -> symbol.translateToObjCObject()
                        KtClassKind.ENUM_CLASS -> symbol.translateToObjCClass()
                        KtClassKind.COMPANION_OBJECT -> symbol.translateToObjCObject()
                        else -> return result
                    } ?: return result

                    symbol.getDeclaredSuperInterfaceSymbols().forEach { superInterfaceSymbol ->
                        result.addAll(process(superInterfaceSymbol, true))
                    }

                    symbol.getSuperClassSymbolNotAny()?.let { superClassSymbol ->
                        process(superClassSymbol, true)
                    }

                    result.add(translatedObjCClassOrProtocol)
                    translatedObjCClassOrProtocol
                }

                if (forwardProtocolsAndClasses) {
                    when (stub) {
                        is ObjCInterface -> classForwardDeclarations.add(ObjCClassForwardDeclaration(stub.name, stub.generics))
                        is ObjCProtocol -> protocolForwardDeclarations.add(stub.name)
                    }
                }
            }
        }
        return result
    }

    fun processDeclaredSymbols(symbols: List<KtSymbol>): List<ObjCExportStub> {
        val result = mutableListOf<ObjCExportStub>()
        symbolTranslationQueue.addAll(symbols)
        while (true) {
            val next = symbolTranslationQueue.removeFirstOrNull() ?: break
            result.addAll(process(next))
        }
        return result
    }

    fun processDependencySymbols(stubs: List<ObjCExportStub>): List<ObjCExportStub> {
        val dependencyClassSymbols = stubs.closureSequence()
            .mapNotNull { stub ->
                when (stub) {
                    is ObjCMethod -> stub.returnType
                    is ObjCParameter -> stub.type
                    is ObjCProperty -> stub.type
                    is ObjCTopLevel -> null
                }
            }
            .flatMap { type ->
                if (type is ObjCClassType) type.typeArguments + type
                else listOf(type)
            }
            .mapNotNull { if (it is ObjCReferenceType) it.classId else null }
            .mapNotNull { classId -> getClassOrObjectSymbolByClassId(classId) }
            .toList()

        symbolTranslationQueue.addAll(dependencyClassSymbols)

        val result = dependencyClassSymbols.flatMap { symbol ->
            process(symbol, forwardProtocolsAndClasses = true)
        }

        return if (result.isNotEmpty()) result + processDependencySymbols(result)
        else result
    }

    val fileSymbols = files.sortedWith(StableFileOrder).map { it.getFileSymbol() }
    val declaredStubs = processDeclaredSymbols(fileSymbols)
    val dependencyStubs = processDependencySymbols(declaredStubs)

    stubs.addAll(declaredStubs + dependencyStubs)

    if (stubs.hasErrorTypes()) {
        stubs.add(errorInterface)
        classForwardDeclarations.add(errorForwardClass)
    }

    if (configuration.generateBaseDeclarationStubs) {
        stubs.addAll(0, objCBaseDeclarations())
    }

    return ObjCHeader(
        stubs = stubs,
        classForwardDeclarations = classForwardDeclarations,
        protocolForwardDeclarations = protocolForwardDeclarations,
        additionalImports = emptyList()
    )
}