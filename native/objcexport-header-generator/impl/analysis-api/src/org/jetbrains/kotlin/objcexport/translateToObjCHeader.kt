/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.allDirectDependencies
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.native.analysis.api.readKlibDeclarationAddresses
import org.jetbrains.kotlin.objcexport.analysisApiUtils.*
import org.jetbrains.kotlin.objcexport.extras.originClassId
import org.jetbrains.kotlin.objcexport.extras.requiresForwardDeclaration
import org.jetbrains.kotlin.objcexport.extras.throwsAnnotationClassIds
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.tooling.core.closure


context(KtAnalysisSession, KtObjCExportSession)
fun translateToObjCHeader(files: List<KtFile>): ObjCHeader {
    val generator = KtObjCExportHeaderGenerator()

    val klibDeclarationAddresses = useSiteModule.closure { module -> module.allDirectDependencies().asIterable() }
        .filterIsInstance<KtLibraryModule>()
        .filter { it.getObjCKotlinModuleName() in configuration.exportedModuleNames }
        .flatMap { it.readKlibDeclarationAddresses().orEmpty() }

    val unresolvedFiles = files.map { ktFile -> KtObjCExportFile(ktFile) } +
        createKtObjCExportFiles(klibDeclarationAddresses)

    generator.translateAll(unresolvedFiles.sortedWith(StableFileOrder))
    return generator.buildObjCHeader()
}

/**
 * Encapsulates the 'dynamic' nature of the ObjCExport where only during the export phase decisions about
 * 1) Which symbols are to be exported
 * 2) In which order symbols are to be exported
 *
 * can be made.
 *
 * Functions inside this class will have side effects such as mutating the [classDeque] or adding results to the [objCStubs]
 */
private class KtObjCExportHeaderGenerator {
    /**
     * Represents a queue containing pointers to all classes that are 'to be translated later'.
     * This happens, e.g., when a class is referenced inside a callable signature. Such 'dependency types' are to be
     * translated
     */
    private val classDeque = ArrayDeque<ClassId>()

    /**
     * The mutable aggregate of the already translated elements
     */
    private val objCStubs = mutableListOf<ObjCTopLevel>()

    /**
     * An index of all already translated classes. All classes here are also present in [objCStubs]
     */
    private val objCStubsByClassId = hashMapOf<ClassId, ObjCClass?>()

    /**
     * An index of already translated classes (by ObjC name)
     */
    private val objCStubsByClassName = hashMapOf<String, ObjCClass>()

    /**
     * The mutable aggregate of all protocol names that shall later be rendered as forward declarations
     */
    private val objCProtocolForwardDeclarations = mutableSetOf<String>()

    /**
     * The mutable aggregate of all class names that shall later be rendered as forward declarations
     */
    private val objCClassForwardDeclarations = mutableSetOf<String>()

    context(KtAnalysisSession, KtObjCExportSession)
    fun translateAll(files: List<KtObjCExportFile>) {
        /**
         * Step 1: Translate classifiers (class, interface, object, ...)
         */
        files.forEach { file ->
            translateFileClassifiers(file)
        }

        /**
         * Step 2: Translate file facades (see [translateToTopLevelFileFacade], [translateToExtensionFacade])
         * This step has to be done after all classifiers were translated to match the translation order of K1
         */
        files.forEach { file ->
            translateFileFacades(file)
        }

        /**
         * Step 3: Translate dependency classes referenced by Step 1 and Step 2
         * Note: Transitive dependencies will still add to this queue and will be processed until we're finished
         */
        while (true) {
            translateClass(classDeque.removeFirstOrNull() ?: break)
        }
    }

    context(KtAnalysisSession, KtObjCExportSession)
    private fun translateClass(classId: ClassId) {
        val classOrObjectSymbol = getClassOrObjectSymbolByClassId(classId) ?: return
        translateClassOrObjectSymbol(classOrObjectSymbol)
    }

    context(KtAnalysisSession, KtObjCExportSession)
    private fun translateFileClassifiers(file: KtObjCExportFile) {
        val resolvedFile = file.resolve()
        resolvedFile.classifierSymbols.sortedWith(StableClassifierOrder).forEach { classOrObjectSymbol ->
            translateClassOrObjectSymbol(classOrObjectSymbol)
        }
    }

    context(KtAnalysisSession, KtObjCExportSession)
    private fun translateFileFacades(file: KtObjCExportFile) {
        val resolvedFile = file.resolve()

        resolvedFile.translateToObjCExtensionFacades().forEach { facade ->
            objCStubs += facade
            enqueueDependencyClasses(facade)
            objCClassForwardDeclarations += facade.name
        }

        resolvedFile.translateToObjCTopLevelFacade()?.let { topLevelFacade ->
            objCStubs += topLevelFacade
            enqueueDependencyClasses(topLevelFacade)
        }
    }

    context(KtAnalysisSession, KtObjCExportSession)
    private fun translateClassOrObjectSymbol(symbol: KtClassOrObjectSymbol): ObjCClass? {
        /* No classId, no stubs ¯\_(ツ)_/¯ */
        val classId = symbol.classIdIfNonLocal ?: return null

        /* Already processed this class, therefore nothing to do! */
        if (classId in objCStubsByClassId) return objCStubsByClassId[classId]

        /**
         * Translate: Note: Even if the result was 'null', the classId will still be marked as 'handled' by adding it
         * to the [objCStubsByClassId] index.
         */
        val objCClass = symbol.translateToObjCExportStub()
        objCStubsByClassId[classId] = objCClass
        objCClass ?: return null

        /*
        To replicate the translation (and result stub order) of the K1 implementation:
        1) Super interface / superclass symbols have to be translated right away
        2) Super interface / superclass symbol export stubs (result of translation) have to be present in the stubs list before the
        original stub
         */
        symbol.getDeclaredSuperInterfaceSymbols().filter { it.isVisibleInObjC() }.forEach { superInterfaceSymbol ->
            translateClassOrObjectSymbol(superInterfaceSymbol)?.let {
                objCProtocolForwardDeclarations += it.name
            }
        }

        symbol.getSuperClassSymbolNotAny()?.takeIf { it.isVisibleInObjC() }?.let { superClassSymbol ->
            translateClassOrObjectSymbol(superClassSymbol)?.let {
                objCClassForwardDeclarations += it.name
            }
        }


        /* Note: It is important to add *this* stub to the result list only after translating/processing the superclass symbols */
        objCStubs += objCClass
        objCStubsByClassName[objCClass.name] = objCClass
        enqueueDependencyClasses(objCClass)
        return objCClass
    }

    /**
     * Will introspect the given [stub] to collect all used 'dependency' types/classes.
     * Example: Usage of Kotlin Stdlib Type (Array):
     *
     * ```
     * class Foo {
     *      fun createArray(): Array<String> = error("stub")
     * }
     * ```
     *
     * The given symbol "Foo" will reference `Array`. Therefore, the `Array` class has to be translated as well (later)
     * and `Array` has to be registered as forward declaration.
     */
    private fun enqueueDependencyClasses(stub: ObjCExportStub) {
        classDeque += stub.closureSequence()
            .flatMap { child -> child.throwsAnnotationClassIds.orEmpty() }

        classDeque += stub.closureSequence()
            .mapNotNull { child ->
                when (child) {
                    is ObjCMethod -> child.returnType
                    is ObjCParameter -> child.type
                    is ObjCProperty -> child.type
                    is ObjCTopLevel -> null
                }
            }
            .flatMap { type ->
                if (type is ObjCClassType) type.typeArguments + type
                else listOf(type)
            }
            .filterIsInstance<ObjCReferenceType>()
            .onEach { type ->
                if (!type.requiresForwardDeclaration) return@onEach
                val nonNullType = if (type is ObjCNullableReferenceType) type.nonNullType else type
                if (nonNullType is ObjCClassType) objCClassForwardDeclarations += nonNullType.className
                if (nonNullType is ObjCProtocolType) objCProtocolForwardDeclarations += nonNullType.protocolName
            }
            .mapNotNull { it.originClassId }
    }

    /**
     * [objCClassForwardDeclarations] are recorded by their respective class name:
     * This method will resolve the objc interface that was translated, which is associated with the [className] and
     * build the respective [ObjCClassForwardDeclaration] from it.
     *
     * If no such class was explicitly translated a simple [ObjCClassForwardDeclaration] will be emitted that does not
     * carry any generics.
     */
    private fun resolveObjCClassForwardDeclaration(className: String): ObjCClassForwardDeclaration {
        objCStubsByClassName[className]
            .let { it as? ObjCInterface }
            ?.let { objCClass -> return ObjCClassForwardDeclaration(objCClass.name, objCClass.generics) }

        return ObjCClassForwardDeclaration(className)
    }

    context(KtAnalysisSession, KtObjCExportSession)
    fun buildObjCHeader(): ObjCHeader {
        val hasErrorTypes = objCStubs.hasErrorTypes()

        val protocolForwardDeclarations = objCProtocolForwardDeclarations.toSet()

        val classForwardDeclarations = objCClassForwardDeclarations
            .map { className -> resolveObjCClassForwardDeclaration(className) }
            .toSet()

        val stubs = (if (configuration.generateBaseDeclarationStubs) objCBaseDeclarations() else emptyList()).plus(objCStubs)
            .plus(listOfNotNull(errorInterface.takeIf { hasErrorTypes }))

        return ObjCHeader(
            stubs = stubs,
            classForwardDeclarations = classForwardDeclarations,
            protocolForwardDeclarations = protocolForwardDeclarations,
            additionalImports = emptyList()
        )
    }
}