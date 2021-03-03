/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree

import com.intellij.util.containers.FactoryMap
import kotlinx.metadata.*
import kotlinx.metadata.klib.KlibModuleMetadata
import kotlinx.metadata.klib.fqName
import kotlinx.metadata.klib.klibEnumEntries
import org.jetbrains.kotlin.descriptors.commonizer.CommonizerParameters
import org.jetbrains.kotlin.descriptors.commonizer.LeafCommonizerTarget
import org.jetbrains.kotlin.descriptors.commonizer.ModulesProvider.ModuleInfo
import org.jetbrains.kotlin.descriptors.commonizer.TargetProvider
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirEntityId
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirName
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirPackageName
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.*
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ClassesToProcess.ClassEntry
import org.jetbrains.kotlin.descriptors.commonizer.metadata.utils.SerializedMetadataLibraryProvider
import org.jetbrains.kotlin.descriptors.commonizer.prettyName
import org.jetbrains.kotlin.descriptors.commonizer.utils.*
import org.jetbrains.kotlin.storage.StorageManager

/**
 * N.B. Limitations on C/Obj-C interop.
 *
 * [Case 1]: An interop library with two fragments for two targets. The first fragment has a forward declaration of classifier A.
 * The second one has a definition of class A. Both fragments have a top-level callable (ex: function)
 * with the same signature that refers to type "A" as its return type.
 *
 * What will happen: Forward declarations will be ignored during building CIR merged tree. So the node for class A
 * will contain CirClass "A" for the second target only. This node will not succeed in commonization, and no common class
 * declaration will be produced. As a result the top-level callable will not be commonized, as it refers to the type "A"
 * that is not formally commonized.
 *
 * This is not strictly correct: The classifier "A" exists in both targets though in different form. So if the user
 * would write shared source code that uses "A" and the callable, then this code would successfully compile against both targets.
 *
 * The reason why commonization of such classifiers is not supported yet is that this is quite a rare case that requires
 * a complex implementation with potential performance penalty.
 *
 * [Case 2]: A library with two fragments for two targets. The first fragment is interop. The second one is not.
 * Similarly to case 1, the 1st fragment has a forward declaration of a classifier, and the 2nd has a real classifier.
 *
 * At the moment, this is an exotic case. It could happen if someone tries to commonize an MPP library for Native and non-Native
 * targets (which is not supported yet), or a Native library where one fragment is produced via C-interop tool and the other one
 * is compiled from Kotlin/Native source code (not sure this should be supported at all).
 */
class CirTreeMerger(
    private val storageManager: StorageManager,
    private val classifiers: CirKnownClassifiers,
    private val parameters: CommonizerParameters
) {
    class CirTreeMergeResult(
        val root: CirRootNode,
        val missingModuleInfos: Map<LeafCommonizerTarget, Collection<ModuleInfo>>
    )

    private class CirTreeMergingContext(
        val targetIndex: Int,
        val typeResolver: CirTypeResolver
    ) {
        fun create(classEntry: ClassEntry): CirTreeMergingContext = when (classEntry) {
            is ClassEntry.RegularClassEntry -> create(classEntry.clazz.typeParameters)
            is ClassEntry.EnumEntry -> this
        }

        fun create(typeParameters: List<KmTypeParameter>): CirTreeMergingContext {
            val newTypeResolver = typeResolver.create(typeParameters)
            return if (newTypeResolver !== typeResolver)
                CirTreeMergingContext(targetIndex, newTypeResolver)
            else
                this
        }
    }

    private val leafTargetsSize = parameters.targetProviders.size

    fun build(): CirTreeMergeResult {
        val result = processRoot()
        System.gc()
        return result
    }

    private fun processRoot(): CirTreeMergeResult {
        val rootNode: CirRootNode = buildRootNode(storageManager, leafTargetsSize)

        // remember any exported forward declarations from common fragments of dependee modules
        parameters.dependencyModulesProvider?.loadModuleInfos()?.forEach(::processCInteropModuleAttributes)

        val commonModuleNames = parameters.getCommonModuleNames()
        val missingModuleInfosByTargets = mutableMapOf<LeafCommonizerTarget, Collection<ModuleInfo>>()

        parameters.targetProviders.forEachIndexed { targetIndex, targetProvider ->
            val allModuleInfos = targetProvider.modulesProvider.loadModuleInfos()

            val (commonModuleInfos, missingModuleInfos) = allModuleInfos.partition { it.name in commonModuleNames }
            processTarget(targetIndex, rootNode, targetProvider, commonModuleInfos)

            missingModuleInfosByTargets[targetProvider.target] = missingModuleInfos

            parameters.progressLogger?.invoke("Loaded declarations for ${targetProvider.target.prettyName}")
            System.gc()
        }

        return CirTreeMergeResult(
            root = rootNode,
            missingModuleInfos = missingModuleInfosByTargets
        )
    }

    private fun processTarget(
        targetIndex: Int,
        rootNode: CirRootNode,
        targetProvider: TargetProvider,
        commonModuleInfos: Collection<ModuleInfo>
    ) {
        rootNode.targetDeclarations[targetIndex] = CirRootFactory.create(targetProvider.target)

        if (commonModuleInfos.isEmpty())
            return

        val context = CirTreeMergingContext(
            targetIndex = targetIndex,
            // all classifiers "visible" for the target:
            typeResolver = CirTypeResolver.create(
                providedClassifiers = CirProvidedClassifiers.of(
                    classifiers.commonDependencies,
                    CirProvidedClassifiers.by(targetProvider.dependencyModulesProvider),
                    CirProvidedClassifiers.by(targetProvider.modulesProvider)
                )
            )
        )

        commonModuleInfos.forEach { moduleInfo ->
            val metadata = targetProvider.modulesProvider.loadModuleMetadata(moduleInfo.name)
            val module = KlibModuleMetadata.read(SerializedMetadataLibraryProvider(metadata))
            processModule(context, rootNode, moduleInfo, module)
        }
    }

    private fun processModule(
        context: CirTreeMergingContext,
        rootNode: CirRootNode,
        moduleInfo: ModuleInfo,
        module: KlibModuleMetadata
    ) {
        processCInteropModuleAttributes(moduleInfo)

        val moduleName: CirName = CirName.create(module.name)
        val moduleNode: CirModuleNode = rootNode.modules.getOrPut(moduleName) {
            buildModuleNode(storageManager, leafTargetsSize)
        }
        moduleNode.targetDeclarations[context.targetIndex] = CirModuleFactory.create(moduleName)

        val groupedFragments: Map<CirPackageName, Collection<KmModuleFragment>> = module.fragments.foldToMap { fragment ->
            fragment.fqName?.let(CirPackageName::create) ?: error("A fragment without FQ name in module $moduleName: $fragment")
        }

        groupedFragments.forEach { (packageName, fragments) ->
            processFragments(context, moduleNode, fragments, packageName)
        }
    }

    private fun processFragments(
        context: CirTreeMergingContext,
        moduleNode: CirModuleNode,
        fragments: Collection<KmModuleFragment>,
        packageName: CirPackageName
    ) {
        val packageNode: CirPackageNode = moduleNode.packages.getOrPut(packageName) {
            buildPackageNode(storageManager, leafTargetsSize)
        }
        packageNode.targetDeclarations[context.targetIndex] = CirPackageFactory.create(packageName)

        val classesToProcess = ClassesToProcess()
        fragments.forEach { fragment ->
            classesToProcess.addClassesFromFragment(fragment)

            fragment.pkg?.let { pkg ->
                pkg.properties.forEach { property ->
                    val propertyContext = context.create(property.typeParameters)
                    processProperty(propertyContext, packageNode, property)
                }
                pkg.functions.forEach { function ->
                    val functionContext = context.create(function.typeParameters)
                    processFunction(functionContext, packageNode, function)
                }
                pkg.typeAliases.forEach { typeAlias ->
                    val typeAliasContext = context.create(typeAlias.typeParameters)
                    processTypeAlias(typeAliasContext, packageNode, typeAlias)
                }
            }
        }

        classesToProcess.forEachClassInScope(parentClassId = null) { classEntry ->
            val classContext = context.create(classEntry)
            processClass(classContext, packageNode, classEntry, classesToProcess)
        }
    }

    private fun processProperty(
        context: CirTreeMergingContext,
        ownerNode: CirNodeWithMembers<*, *>,
        property: KmProperty
    ) {
        if (property.isFakeOverride())
            return

        val maybeClassOwnerNode: CirClassNode? = ownerNode as? CirClassNode

        val approximationKey = PropertyApproximationKey(property, context.typeResolver)
        val propertyNode: CirPropertyNode = ownerNode.properties.getOrPut(approximationKey) {
            buildPropertyNode(storageManager, leafTargetsSize, classifiers, maybeClassOwnerNode?.commonDeclaration)
        }
        propertyNode.targetDeclarations[context.targetIndex] = CirPropertyFactory.create(
            name = approximationKey.name,
            source = property,
            containingClass = maybeClassOwnerNode?.targetDeclarations?.get(context.targetIndex),
            typeResolver = context.typeResolver
        )
    }

    private fun processFunction(
        context: CirTreeMergingContext,
        ownerNode: CirNodeWithMembers<*, *>,
        function: KmFunction
    ) {
        if (function.isFakeOverride()
            || function.isKniBridgeFunction()
            || function.isTopLevelDeprecatedFunction(isTopLevel = ownerNode !is CirClassNode)
        ) {
            return
        }

        val maybeClassOwnerNode: CirClassNode? = ownerNode as? CirClassNode

        val approximationKey = FunctionApproximationKey(function, context.typeResolver)
        val functionNode: CirFunctionNode = ownerNode.functions.getOrPut(approximationKey) {
            buildFunctionNode(storageManager, leafTargetsSize, classifiers, maybeClassOwnerNode?.commonDeclaration)
        }
        functionNode.targetDeclarations[context.targetIndex] = CirFunctionFactory.create(
            name = approximationKey.name,
            source = function,
            containingClass = maybeClassOwnerNode?.targetDeclarations?.get(context.targetIndex),
            typeResolver = context.typeResolver
        )
    }

    private fun processClass(
        context: CirTreeMergingContext,
        ownerNode: CirNodeWithMembers<*, *>,
        classEntry: ClassEntry,
        classesToProcess: ClassesToProcess
    ) {
        val classId = classEntry.classId
        val className = classId.relativeNameSegments.last()

        val maybeClassOwnerNode: CirClassNode? = ownerNode as? CirClassNode
        val classNode: CirClassNode = ownerNode.classes.getOrPut(className) {
            buildClassNode(storageManager, leafTargetsSize, classifiers, maybeClassOwnerNode?.commonDeclaration, classId)
        }

        val clazz: KmClass?
        val isEnumEntry: Boolean

        classNode.targetDeclarations[context.targetIndex] = when (classEntry) {
            is ClassEntry.RegularClassEntry -> {
                clazz = classEntry.clazz
                isEnumEntry = Flag.Class.IS_ENUM_ENTRY(clazz.flags)

                CirClassFactory.create(className, clazz, context.typeResolver)
            }
            is ClassEntry.EnumEntry -> {
                clazz = null
                isEnumEntry = true

                CirClassFactory.createDefaultEnumEntry(
                    name = className,
                    annotations = classEntry.annotations,
                    enumClassId = classEntry.enumClassId,
                    enumClass = classEntry.enumClass,
                    typeResolver = context.typeResolver
                )
            }
        }

        if (!isEnumEntry) {
            clazz?.constructors?.forEach { constructor ->
                // TODO: nowhere to read constructor type parameters from
                //val constructorContext = context.create(constructor.typeParameters)

                processClassConstructor(context, classNode, constructor)
            }
        }

        clazz?.properties?.forEach { property ->
            val propertyContext = context.create(property.typeParameters)
            processProperty(propertyContext, classNode, property)
        }
        clazz?.functions?.forEach { function ->
            val functionContext = context.create(function.typeParameters)
            processFunction(functionContext, classNode, function)
        }

        classesToProcess.forEachClassInScope(parentClassId = classId) { nestedClassEntry ->
            val nestedClassContext = context.create(nestedClassEntry)
            processClass(nestedClassContext, classNode, nestedClassEntry, classesToProcess)
        }
    }

    private fun processClassConstructor(
        context: CirTreeMergingContext,
        classNode: CirClassNode,
        constructor: KmConstructor
    ) {
        val approximationKey = ConstructorApproximationKey(constructor, context.typeResolver)
        val constructorNode: CirClassConstructorNode = classNode.constructors.getOrPut(approximationKey) {
            buildClassConstructorNode(storageManager, leafTargetsSize, classifiers, classNode.commonDeclaration)
        }
        constructorNode.targetDeclarations[context.targetIndex] = CirClassConstructorFactory.create(
            source = constructor,
            containingClass = classNode.targetDeclarations[context.targetIndex]!!,
            typeResolver = context.typeResolver
        )
    }

    private fun processTypeAlias(
        context: CirTreeMergingContext,
        packageNode: CirPackageNode,
        typeAlias: KmTypeAlias
    ) {
        val typeAliasName = CirName.create(typeAlias.name)
        val typeAliasId = CirEntityId.create(packageNode.packageName, typeAliasName)

        val typeAliasNode: CirTypeAliasNode = packageNode.typeAliases.getOrPut(typeAliasName) {
            buildTypeAliasNode(storageManager, leafTargetsSize, classifiers, typeAliasId)
        }
        typeAliasNode.targetDeclarations[context.targetIndex] = CirTypeAliasFactory.create(
            name = typeAliasName,
            source = typeAlias,
            typeResolver = context.typeResolver
        )
    }

    private fun processCInteropModuleAttributes(moduleInfo: ModuleInfo) {
        val cInteropAttributes = moduleInfo.cInteropAttributes ?: return
        val exportForwardDeclarations = cInteropAttributes.exportForwardDeclarations.takeIf { it.isNotEmpty() } ?: return

        exportForwardDeclarations.forEach { classFqName ->
            // Class has synthetic package FQ name (cnames/objcnames). Need to transfer it to the main package.
            val packageName = CirPackageName.create(classFqName.substringBeforeLast('.', missingDelimiterValue = ""))
            val className = CirName.create(classFqName.substringAfterLast('.'))

            classifiers.forwardDeclarations.addExportedForwardDeclaration(CirEntityId.create(packageName, className))
        }
    }
}

private class ClassesToProcess {
    sealed class ClassEntry {
        abstract val classId: CirEntityId

        data class RegularClassEntry(
            override val classId: CirEntityId,
            val clazz: KmClass
        ) : ClassEntry()

        data class EnumEntry(
            override val classId: CirEntityId,
            val annotations: List<KmAnnotation>,
            val enumClassId: CirEntityId,
            val enumClass: KmClass
        ) : ClassEntry()
    }

    // key = parent class ID (or NON_EXISTING_CLASSIFIER_ID for top-level classes)
    // value = classes under this parent class (MutableList to preserve order of classes)
    private val groupedByParentClassId = FactoryMap.create<CirEntityId, MutableList<ClassEntry>> { ArrayList() }

    fun addClassesFromFragment(fragment: KmModuleFragment) {
        val klibEnumEntries = LinkedHashMap<CirEntityId, ClassEntry.EnumEntry>() // linked hash map to preserve order
        val regularClassIds = HashSet<CirEntityId>()

        fragment.classes.forEach { clazz ->
            val classId: CirEntityId = CirEntityId.create(clazz.name)
            val parentClassId: CirEntityId = classId.getParentEntityId() ?: NON_EXISTING_CLASSIFIER_ID

            if (Flag.Class.IS_ENUM_CLASS(clazz.flags)) {
                clazz.klibEnumEntries.forEach { entry ->
                    val enumEntryId = classId.createNestedEntityId(CirName.create(entry.name))
                    klibEnumEntries[enumEntryId] = ClassEntry.EnumEntry(enumEntryId, entry.annotations, classId, clazz)
                }
            }

            groupedByParentClassId.getValue(parentClassId) += ClassEntry.RegularClassEntry(classId, clazz)
            regularClassIds += classId
        }

        // add enum entries that are not stored in module as KmClass records
        klibEnumEntries.forEach { (enumEntryId, enumEntry) ->
            if (enumEntryId !in regularClassIds) {
                groupedByParentClassId.getValue(enumEntry.enumClassId) += enumEntry
            }
        }
    }

    fun forEachClassInScope(parentClassId: CirEntityId?, block: (ClassEntry) -> Unit) {
        groupedByParentClassId[parentClassId ?: NON_EXISTING_CLASSIFIER_ID]?.forEach { classEntry -> block(classEntry) }
    }
}
