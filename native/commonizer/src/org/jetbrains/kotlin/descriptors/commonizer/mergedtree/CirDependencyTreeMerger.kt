/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.commonizer.CommonizerParameters
import org.jetbrains.kotlin.descriptors.commonizer.ModulesProvider
import org.jetbrains.kotlin.descriptors.commonizer.TargetProvider
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirClass
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.*
import org.jetbrains.kotlin.descriptors.commonizer.utils.intern
import org.jetbrains.kotlin.descriptors.commonizer.utils.internedClassId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.NullableLazyValue
import org.jetbrains.kotlin.storage.StorageManager

class CirDependencyTreeMerger(
    private val storageManager: StorageManager,
    private val classifiers: CirKnownClassifiers,
    private val parameters: CommonizerParameters,
) {

    private val size = parameters.targetProviders.size

    fun merge(): CirTreeMerger.CirTreeMergeResult {
        return try {
            processRoot()
        } finally {
            System.gc()
        }
    }

    fun processRoot(): CirTreeMerger.CirTreeMergeResult {
        val rootNode: CirRootNode = buildRootNode(storageManager, size)

        // remember any exported forward declarations from common fragments of dependee modules
        parameters.dependeeModulesProvider?.loadModuleInfos()?.values?.forEach(::processCInteropModuleAttributes)

        // load common dependencies
        val dependencyModules = parameters.dependeeModulesProvider?.loadModules(emptyList())?.values.orEmpty()

        val moduleInfos: List<Map<String, ModulesProvider.ModuleInfo>> = parameters.targetProviders
            .mapNotNull { targetProvider -> targetProvider.dependeeModulesProvider?.loadModuleInfos() }

        val commonModuleNames = moduleInfos.map { it.keys }.reduce { a, b -> a intersect b }

        parameters.targetProviders.forEachIndexed { targetIndex, targetProvider ->
            val commonModuleInfos = moduleInfos[targetIndex].filterKeys { it in commonModuleNames }
            processTarget(rootNode, targetIndex, targetProvider, commonModuleInfos, dependencyModules)
            parameters.progressLogger?.invoke("Loaded dependency declarations for [${targetProvider.target.name}]")
            System.gc()
        }

        val missingModuleInfos = moduleInfos.mapIndexed { index, infos ->
            val target = parameters.targetProviders[index].target
            val missingInfos = infos.filterKeys { name -> name !in commonModuleNames }.values
            target to missingInfos
        }.toMap()

        return CirTreeMerger.CirTreeMergeResult(
            root = rootNode,
            missingModuleInfos = missingModuleInfos
        )
    }


    private fun processTarget(
        rootNode: CirRootNode,
        targetIndex: Int,
        targetProvider: TargetProvider,
        commonModuleInfos: Map<String, ModulesProvider.ModuleInfo>,
        dependeeModules: Collection<ModuleDescriptor>
    ) {
        rootNode.targetDeclarations[targetIndex] = CirRootFactory.create(
            targetProvider.target,
            targetProvider.builtInsClass.name,
            targetProvider.builtInsProvider
        )

        val moduleDescriptors: Map<String, ModuleDescriptor> =
            targetProvider.dependeeModulesProvider?.loadModules(dependeeModules).orEmpty()
        val modules: MutableMap<Name, CirModuleNode> = rootNode.modules

        moduleDescriptors.forEach { (name, moduleDescriptor) ->
            val moduleInfo = commonModuleInfos[name] ?: return@forEach
            processModule(modules, targetIndex, moduleInfo, moduleDescriptor)
        }
    }

    private fun processModule(
        modules: MutableMap<Name, CirModuleNode>,
        targetIndex: Int,
        moduleInfo: ModulesProvider.ModuleInfo,
        moduleDescriptor: ModuleDescriptor
    ) {
        processCInteropModuleAttributes(moduleInfo)

        val moduleName: Name = moduleDescriptor.name.intern()
        val moduleNode: CirModuleNode = modules.getOrPut(moduleName) {
            buildModuleNode(storageManager, size)
        }
        moduleNode.targetDeclarations[targetIndex] = CirModuleFactory.create(moduleDescriptor)

        val packages: MutableMap<FqName, CirPackageNode> = moduleNode.packages

        moduleDescriptor.collectNonEmptyPackageMemberScopes { packageFqName, packageMemberScope ->
            processPackage(packages, targetIndex, packageFqName.intern(), packageMemberScope, moduleName)
        }
    }

    private fun processPackage(
        packages: MutableMap<FqName, CirPackageNode>,
        targetIndex: Int,
        packageFqName: FqName,
        packageMemberScope: MemberScope,
        moduleName: Name
    ) {
        val packageNode: CirPackageNode = packages.getOrPut(packageFqName) {
            buildPackageNode(storageManager, size, packageFqName, moduleName)
        }
        packageNode.targetDeclarations[targetIndex] = CirPackageFactory.create(packageFqName)

        val properties: MutableMap<PropertyApproximationKey, CirPropertyNode> = packageNode.properties
        val functions: MutableMap<FunctionApproximationKey, CirFunctionNode> = packageNode.functions
        val classes: MutableMap<Name, CirClassNode> = packageNode.classes
        val typeAliases: MutableMap<Name, CirTypeAliasNode> = packageNode.typeAliases

        packageMemberScope.collectMembers(
            PropertyCollector { propertyDescriptor ->
                processProperty(properties, targetIndex, propertyDescriptor, null)
            },
            FunctionCollector { functionDescriptor ->
                processFunction(functions, targetIndex, functionDescriptor, null)
            },
            ClassCollector { classDescriptor ->
                processClass(classes, targetIndex, classDescriptor, null) { className ->
                    internedClassId(packageFqName, className)
                }
            },
            TypeAliasCollector { typeAliasDescriptor ->
                processTypeAlias(typeAliases, targetIndex, typeAliasDescriptor, packageFqName)
            }
        )
    }

    private fun processProperty(
        properties: MutableMap<PropertyApproximationKey, CirPropertyNode>,
        targetIndex: Int,
        propertyDescriptor: PropertyDescriptor,
        parentCommonDeclaration: NullableLazyValue<*>?
    ) {
        val propertyNode: CirPropertyNode = properties.getOrPut(PropertyApproximationKey(propertyDescriptor)) {
            buildPropertyNode(storageManager, size, classifiers, parentCommonDeclaration)
        }
        propertyNode.targetDeclarations[targetIndex] = CirPropertyFactory.create(propertyDescriptor)
    }

    private fun processFunction(
        functions: MutableMap<FunctionApproximationKey, CirFunctionNode>,
        targetIndex: Int,
        functionDescriptor: SimpleFunctionDescriptor,
        parentCommonDeclaration: NullableLazyValue<*>?
    ) {
        val functionNode: CirFunctionNode = functions.getOrPut(FunctionApproximationKey(functionDescriptor)) {
            buildFunctionNode(storageManager, size, classifiers, parentCommonDeclaration)
        }
        functionNode.targetDeclarations[targetIndex] = CirFunctionFactory.create(functionDescriptor)
    }

    private fun processClass(
        classes: MutableMap<Name, CirClassNode>,
        targetIndex: Int,
        classDescriptor: ClassDescriptor,
        parentCommonDeclaration: NullableLazyValue<*>?,
        classIdFunction: (Name) -> ClassId
    ) {
        val className = classDescriptor.name.intern()
        val classId = classIdFunction(className)

        val classNode: CirClassNode = classes.getOrPut(className) {
            buildClassNode(storageManager, size, classifiers, parentCommonDeclaration, classId)
        }
        classNode.targetDeclarations[targetIndex] = CirClassFactory.create(classDescriptor)

        val parentCommonDeclarationForMembers: NullableLazyValue<CirClass> = classNode.commonDeclaration

        val constructors: MutableMap<ConstructorApproximationKey, CirClassConstructorNode> = classNode.constructors
        val properties: MutableMap<PropertyApproximationKey, CirPropertyNode> = classNode.properties
        val functions: MutableMap<FunctionApproximationKey, CirFunctionNode> = classNode.functions
        val nestedClasses: MutableMap<Name, CirClassNode> = classNode.classes

        classDescriptor.constructors.forEach { processClassConstructor(constructors, targetIndex, it, parentCommonDeclarationForMembers) }

        classDescriptor.unsubstitutedMemberScope.collectMembers(
            PropertyCollector { propertyDescriptor ->
                processProperty(properties, targetIndex, propertyDescriptor, parentCommonDeclarationForMembers)
            },
            FunctionCollector { functionDescriptor ->
                processFunction(functions, targetIndex, functionDescriptor, parentCommonDeclarationForMembers)
            },
            ClassCollector { nestedClassDescriptor ->
                processClass(nestedClasses, targetIndex, nestedClassDescriptor, parentCommonDeclarationForMembers) { nestedClassName ->
                    internedClassId(classId, nestedClassName)
                }
            }
        )
    }

    private fun processClassConstructor(
        constructors: MutableMap<ConstructorApproximationKey, CirClassConstructorNode>,
        targetIndex: Int,
        constructorDescriptor: ClassConstructorDescriptor,
        parentCommonDeclaration: NullableLazyValue<*>?
    ) {
        val constructorNode: CirClassConstructorNode = constructors.getOrPut(ConstructorApproximationKey(constructorDescriptor)) {
            buildClassConstructorNode(storageManager, size, classifiers, parentCommonDeclaration)
        }
        constructorNode.targetDeclarations[targetIndex] = CirClassConstructorFactory.create(constructorDescriptor)
    }

    private fun processTypeAlias(
        typeAliases: MutableMap<Name, CirTypeAliasNode>,
        targetIndex: Int,
        typeAliasDescriptor: TypeAliasDescriptor,
        packageFqName: FqName
    ) {
        val typeAliasName = typeAliasDescriptor.name.intern()
        val typeAliasClassId = internedClassId(packageFqName, typeAliasName)

        val typeAliasNode: CirTypeAliasNode = typeAliases.getOrPut(typeAliasName) {
            buildTypeAliasNode(storageManager, size, classifiers, typeAliasClassId)
        }
        typeAliasNode.targetDeclarations[targetIndex] = CirTypeAliasFactory.create(typeAliasDescriptor)
    }

    private fun processCInteropModuleAttributes(moduleInfo: ModulesProvider.ModuleInfo) {
        val cInteropAttributes = moduleInfo.cInteropAttributes ?: return
        val exportForwardDeclarations = cInteropAttributes.exportForwardDeclarations.takeIf { it.isNotEmpty() } ?: return
        val mainPackageFqName = FqName(cInteropAttributes.mainPackageFqName).intern()

        exportForwardDeclarations.forEach { classFqName ->
            // Class has synthetic package FQ name (cnames/objcnames). Need to transfer it to the main package.
            val className = Name.identifier(classFqName.substringAfterLast('.')).intern()
            classifiers.forwardDeclarations.addExportedForwardDeclaration(internedClassId(mainPackageFqName, className))
        }
    }
}
