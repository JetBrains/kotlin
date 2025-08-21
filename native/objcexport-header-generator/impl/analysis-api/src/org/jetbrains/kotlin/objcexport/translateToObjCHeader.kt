/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.export.utilities.getDeclaredSuperInterfaceSymbols
import org.jetbrains.kotlin.analysis.api.export.utilities.getSuperClassSymbolNotAny
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.objcexport.analysisApiUtils.*
import org.jetbrains.kotlin.objcexport.extras.*
import org.jetbrains.kotlin.objcexport.mangling.mangleClassForwards
import org.jetbrains.kotlin.objcexport.mangling.mangleObjCStubs


fun ObjCExportContext.translateToObjCHeader(
    files: List<KtObjCExportFile>,
    withObjCBaseDeclarations: Boolean = true,
): ObjCHeader {
    val generator = KtObjCExportHeaderGenerator(withObjCBaseDeclarations)
    return with(generator) {
        translateAll(files.sortedWith(StableFileOrder))
        buildObjCHeader()
    }
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
private class KtObjCExportHeaderGenerator(
    private val withObjCBaseDeclarations: Boolean,
) {
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
     * An index of already translated classes by [ObjCClassKey]
     * @see [addObjCStubIfNotTranslated]
     */
    private val objCStubsByClassKey = hashMapOf<ObjCClassKey, ObjCClass>()

    /**
     * The mutable aggregate of all protocol names that shall later be rendered as forward declarations
     */
    private val objCProtocolForwardDeclarations = mutableSetOf<String>()

    /**
     * The mutable aggregate of all class names that shall later be rendered as forward declarations
     */
    private val objCClassForwardDeclarations = mutableSetOf<ObjCClassKey>()


    fun ObjCExportContext.translateAll(files: List<KtObjCExportFile>) {
        /**
         * Step 1: Translate classifiers (class, interface, object, ...)
         */
        files.forEach { file ->
            translateFileClassifiers(file)
        }

        /**
         * Step 2: Translate extensions (see [translateToObjCExtensionFacades])
         * This step has to be done after all classifiers were translated to match the translation order of K1
         */
        translateExtensionsFacades(files)

        /**
         * Step 3: Translate top level callables (see [translateToTopLevelFileFacade])
         * This step has to be done after all classifiers were translated to match the translation order of K1
         */
        files.forEach { file ->
            translateTopLevelFacade(file)
        }

        /**
         * Step 4: Translate dependency classes referenced by Step 1 and Step 2
         * Note: Transitive dependencies will still add to this queue and will be processed until we're finished
         */
        while (true) {
            translateClass(classDeque.removeFirstOrNull() ?: break)
        }
    }


    private fun ObjCExportContext.translateClass(classId: ClassId) {
        val classOrObjectSymbol = analysisSession.findClass(classId) ?: return
        translateClassOrObjectSymbol(classOrObjectSymbol)
    }

    private fun ObjCExportContext.translateFileClassifiers(file: KtObjCExportFile) {
        val resolvedFile = with(file) { analysisSession.resolve() }
        resolvedFile.classifierSymbols.sortedWith(StableClassifierOrder).forEach { classOrObjectSymbol ->
            translateClassOrObjectSymbol(classOrObjectSymbol)
        }
    }

    private fun ObjCExportContext.translateExtensionsFacades(files: List<KtObjCExportFile>) {
        translateToObjCExtensionFacades(files).forEach { symbolToFacade ->
            val symbol = symbolToFacade.key
            val facade = symbolToFacade.value
            translateClassOrObjectSymbol(symbol)
            addObjCStubIfNotTranslated(facade, symbol.classId?.packageFqName?.asString())
            enqueueDependencyClasses(facade)
            objCClassForwardDeclarations += ObjCClassKey(facade.name, symbol.classId?.packageFqName?.asString())
        }
    }

    private fun ObjCExportContext.translateTopLevelFacade(file: KtObjCExportFile) {
        val resolvedFile = with(file) { analysisSession.resolve() }
        translateToObjCTopLevelFacade(resolvedFile)?.let { topLevelFacade ->
            addObjCStubIfNotTranslated(topLevelFacade)
            enqueueDependencyClasses(topLevelFacade)
        }
    }

    private fun ObjCExportContext.translateClassOrObjectSymbol(symbol: KaClassSymbol): ObjCClass? {
        /* No classId, no stubs ¯\_(ツ)_/¯ */
        val classId = symbol.classId ?: return null

        /* Already processed this class, therefore nothing to do! */
        if (classId in objCStubsByClassId) return objCStubsByClassId[classId]

        /**
         * Translate: Note: Even if the result was 'null', the classId will still be marked as 'handled' by adding it
         * to the [objCStubsByClassId] index.
         */
        val objCClass = translateToObjCExportStub(symbol)
        objCStubsByClassId[classId] = objCClass
        objCClass ?: return null

        /*
        To replicate the translation (and result stub order) of the K1 implementation:
        1) Super interface / superclass symbols have to be translated right away
        2) Super interface / superclass symbol export stubs (result of translation) have to be present in the stubs list before the
        original stub
         */
        analysisSession.getDeclaredSuperInterfaceSymbols(symbol).filter { analysisSession.isVisibleInObjC(it) }
            .forEach { superInterfaceSymbol ->
                translateClassOrObjectSymbol(superInterfaceSymbol)?.let {
                    objCProtocolForwardDeclarations += it.name
                }
            }

        analysisSession.getSuperClassSymbolNotAny(symbol)?.takeIf { analysisSession.isVisibleInObjC(it) }?.let { superClassSymbol ->
            translateClassOrObjectSymbol(superClassSymbol)?.let {
                objCClassForwardDeclarations += ObjCClassKey(it.name, superClassSymbol.classId?.packageFqName?.asString())
            }
        }


        /* Note: It is important to add *this* stub to the result list only after translating/processing the superclass symbols */
        addObjCStubIfNotTranslated(objCClass, symbol.classId?.packageFqName?.asString())
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
            .flatMap { childStub ->
                when (childStub) {
                    is ObjCMethod -> listOf(childStub.returnType)
                    is ObjCParameter -> listOf(childStub.type)
                    is ObjCProperty -> listOf(childStub.type)
                    is ObjCInterface -> childStub.superClassGenerics
                    is ObjCTopLevel -> emptyList()
                }
            }.map { type ->
                /**
                 * [ObjCBlockPointerType] can be wrapped into [ObjCNullableReferenceType]
                 * So before traversing [allTypes] we need to unwrap it into non null type
                 */
                if (type is ObjCNullableReferenceType) type.nonNullType
                else type
            }
            .flatMap { type ->
                val typeArguments = when (type) {
                    is ObjCClassType -> type.typeArguments
                    is ObjCBlockPointerType -> type.allTypes()
                    else -> emptyList()
                }
                typeArguments + type
            }
            .filterIsInstance<ObjCReferenceType>()
            .onEach { type ->
                if (!type.requiresForwardDeclaration) return@onEach
                val nonNullType = if (type is ObjCNullableReferenceType) type.nonNullType else type
                if (nonNullType is ObjCClassType) objCClassForwardDeclarations += ObjCClassKey(
                    nonNullType.className,
                    nonNullType.originClassId?.packageFqName?.asString()
                )
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
    private fun resolveObjCClassForwardDeclaration(classKey: ObjCClassKey): ObjCClassForwardDeclaration {
        objCStubsByClassKey[classKey]
            .let { it as? ObjCInterface }
            ?.let { objCClass -> return ObjCClassForwardDeclaration(objCClass.name, objCClass.generics) }

        return ObjCClassForwardDeclaration(classKey.className)
    }

    fun ObjCExportContext.buildObjCHeader(): ObjCHeader {
        val hasErrorTypes = objCStubs.hasErrorTypes()

        val protocolForwardDeclarations = objCProtocolForwardDeclarations.toSet()

        val classForwardDeclarations = objCClassForwardDeclarations
            .map { className ->
                resolveObjCClassForwardDeclaration(className)
            }
            .toSet()

        val stubs = (if (withObjCBaseDeclarations) exportSession.objCBaseDeclarations() else emptyList()).plus(objCStubs)
            .plus(listOfNotNull(exportSession.errorInterface.takeIf { hasErrorTypes }))

        return ObjCHeader(
            stubs = mangleObjCStubs(stubs.sortedWith(ObjCInterfaceOrder)),
            classForwardDeclarations = mangleClassForwards(classForwardDeclarations).sortedBy { it.className }.toSet(),
            protocolForwardDeclarations = protocolForwardDeclarations.sortedBy { it }.toSet(),
            additionalImports = emptyList()
        )
    }

    /**
     * We verify if a class is already translated by checking the equality of its name and category.
     *
     * ObjC categories are used to translate extensions,
     * so having two classes with the same name but different categories is a valid case.
     *
     * ```objective-c
     * @interface Foo
     * @interface Foo (Extension)
     * ```
     *
     * K1 also uses a dedicated hash map, but filtering out is spread across the translation traversal.
     * See the usage of [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportHeaderGenerator.generatedClasses].
     */
    private fun addObjCStubIfNotTranslated(objCClass: ObjCClass, packageFqn: String? = "") {
        val key = ObjCClassKey(objCClass.name, packageFqn, (objCClass as? ObjCInterface)?.categoryName)
        val translatedClass = objCStubsByClassKey[key]
        if (translatedClass != null) return
        objCStubsByClassKey[key] = objCClass
        objCStubs += objCClass
    }
}

private data class ObjCClassKey(
    val className: String,
    val packageFqn: String? = null,
    val categoryName: String? = null,
)