package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.types.KtNonErrorClassType
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.objcexport.*
import org.jetbrains.kotlin.objcexport.analysisApiUtils.ObjCSymbolsProcessingQueue.Group
import org.jetbrains.kotlin.utils.addIfNotNull

class ObjCSymbolsProcessingQueue private constructor(
    private val groups: List<Group>,
) {

    companion object {
        fun newQueue(): ObjCSymbolsProcessingQueue {
            return ObjCSymbolsProcessingQueue(
                listOf(
                    SuperInterfacesAndClassesGroup(),
                    InterfacesAndClassesGroup(),
                    DependenciesGroup()
                )
            )
        }
    }

    abstract class Group {

        lateinit var queue: ObjCSymbolsProcessingQueue
        val result = mutableListOf<ObjCExportStub>()

        context(KtAnalysisSession, KtObjCExportSession)
        abstract fun process(symbol: KtSymbol)

        context(KtAnalysisSession, KtObjCExportSession)
        open fun process(type: KtType) {
            //Ignore
        }
    }

    data class Result(
        val stubs: List<ObjCExportStub>,
        val forwardClasses: Set<ObjCClassForwardDeclaration>,
        val forwardProtocols: Set<String>,
    )

    init {
        groups.forEach { it.queue = this }
    }

    context(KtAnalysisSession, KtObjCExportSession)
    fun process(symbols: Collection<KtSymbol>): Result {
        symbols.forEach { symbol ->
            processSymbols(symbol)
        }
        return mergeGroups()
    }

    context(KtAnalysisSession, KtObjCExportSession)
    fun process(symbol: KtSymbol) {
        processSymbols(symbol)
        mergeGroups()
    }

    context(KtAnalysisSession, KtObjCExportSession)
    fun process(type: KtType) {
        groups.forEach { group -> group.process(type) }
    }

    context(KtAnalysisSession, KtObjCExportSession)
    private fun processSymbols(symbol: KtSymbol) {
        groups.forEach { group -> group.process(symbol) }
    }

    context(KtAnalysisSession, KtObjCExportSession)
    private fun mergeGroups(): Result {

        val stubs = mutableListOf<ObjCExportStub>()
        val protocolForwardDeclarations = mutableSetOf<String>()
        val classForwardDeclarations = mutableSetOf<ObjCClassForwardDeclaration>()

        groups.forEach { group ->
            stubs.addAll(group.result)
        }

        if (stubs.hasErrorTypes()) {
            stubs.add(errorInterface)
            classForwardDeclarations.add(errorForwardClass)
        }

        protocolForwardDeclarations += stubs
            .filterIsInstance<ObjCClass>()
            .flatMap { it.superProtocols }

        classForwardDeclarations += stubs
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

        return Result(stubs, classForwardDeclarations, protocolForwardDeclarations)
    }
}


private class SuperInterfacesAndClassesGroup : Group() {

    context(KtAnalysisSession, KtObjCExportSession)
    override fun process(symbol: KtSymbol) {
        if (symbol is KtClassOrObjectSymbol) {
            symbol.getDeclaredSuperInterfaceSymbols().forEach { superInterfaceSymbol ->
                result.addIfNotNull(translateClassifier(superInterfaceSymbol))
                queue.process(superInterfaceSymbol)
            }
        }
    }
}

private class InterfacesAndClassesGroup : Group() {

    context(KtAnalysisSession, KtObjCExportSession)
    override fun process(symbol: KtSymbol) {
        result.addIfNotNull(translateClassifier(symbol))
    }
}

private class DependenciesGroup : Group() {

    private val translatedFqNames = mutableSetOf<String>()

    context(KtAnalysisSession, KtObjCExportSession)
    override fun process(symbol: KtSymbol) {
        if (symbol is KtClassOrObjectSymbol) {
            symbol.getDeclaredSuperInterfaceSymbols().forEach {
                collect(symbol)
            }
        }
    }

    context(KtAnalysisSession, KtObjCExportSession)
    override fun process(type: KtType) {
        val fullyExpandedType = type.fullyExpandedType

        if (fullyExpandedType is KtNonErrorClassType) {
            val classId = fullyExpandedType.classId
            val classSymbol = getClassOrObjectSymbolByClassId(classId)
            if (classSymbol != null) collect(classSymbol)
        }
    }

    context(KtAnalysisSession, KtObjCExportSession)
    fun collect(symbol: KtClassOrObjectSymbol) {
        val fqName = symbol.classIdIfNonLocal?.asFqNameString()

        if (symbol.isDeclaredInModule) return
        if (fqName == null) return

        if (translatedFqNames.add(fqName)) {
            result.addIfNotNull(translateClassifier(symbol))
        }
    }
}

context(KtAnalysisSession, KtObjCExportSession)
private fun translateClassifier(symbol: KtSymbol): ObjCExportStub? {
    return if (symbol is KtClassOrObjectSymbol) {
        when (symbol.classKind) {
            KtClassKind.INTERFACE -> symbol.translateToObjCProtocol()
            KtClassKind.CLASS -> symbol.translateToObjCClass()
            KtClassKind.OBJECT -> symbol.translateToObjCObject()
            else -> null
        }
    } else {
        null
    }
}