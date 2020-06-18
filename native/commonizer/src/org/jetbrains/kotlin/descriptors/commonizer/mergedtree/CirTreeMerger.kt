/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.commonizer.Parameters
import org.jetbrains.kotlin.descriptors.commonizer.TargetProvider
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirClass
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.*
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirRootNode.CirClassifiersCacheImpl
import org.jetbrains.kotlin.descriptors.commonizer.utils.intern
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.NullableLazyValue
import org.jetbrains.kotlin.storage.StorageManager

class CirTreeMerger(
    private val storageManager: StorageManager,
    private val parameters: Parameters
) {
    private val size = parameters.targetProviders.size
    private lateinit var cacheRW: CirClassifiersCacheImpl

    fun merge(): CirRootNode {
        val rootNode: CirRootNode = buildRootNode(storageManager, size)
        cacheRW = rootNode.cache

        parameters.targetProviders.forEachIndexed { targetIndex, targetProvider ->
            processTarget(rootNode, targetIndex, targetProvider)
            parameters.progressLogger?.invoke("Loaded declarations for [${targetProvider.target.name}]")
            System.gc()
        }

        return rootNode
    }

    private fun processTarget(
        rootNode: CirRootNode,
        targetIndex: Int,
        targetProvider: TargetProvider
    ) {
        rootNode.targetDeclarations[targetIndex] = CirRootFactory.create(
            targetProvider.target,
            targetProvider.builtInsClass.name,
            targetProvider.builtInsProvider
        )

        val moduleDescriptors: Collection<ModuleDescriptor> = targetProvider.modulesProvider.loadModules()
        val modules: MutableMap<Name, CirModuleNode> = rootNode.modules

        moduleDescriptors.forEach { moduleDescriptor ->
            processModule(modules, targetIndex, moduleDescriptor)
        }
    }

    private fun processModule(
        modules: MutableMap<Name, CirModuleNode>,
        targetIndex: Int,
        moduleDescriptor: ModuleDescriptor
    ) {
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
            PropertyCollector { processProperty(properties, targetIndex, it, null) },
            FunctionCollector { processFunction(functions, targetIndex, it, null) },
            ClassCollector { processClass(classes, targetIndex, it, null) },
            TypeAliasCollector { processTypeAlias(typeAliases, targetIndex, it) }
        )
    }

    private fun processProperty(
        properties: MutableMap<PropertyApproximationKey, CirPropertyNode>,
        targetIndex: Int,
        propertyDescriptor: PropertyDescriptor,
        parentCommonDeclaration: NullableLazyValue<*>?
    ) {
        val propertyNode: CirPropertyNode = properties.getOrPut(PropertyApproximationKey(propertyDescriptor)) {
            buildPropertyNode(storageManager, size, cacheRW, parentCommonDeclaration)
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
            buildFunctionNode(storageManager, size, cacheRW, parentCommonDeclaration)
        }
        functionNode.targetDeclarations[targetIndex] = CirFunctionFactory.create(functionDescriptor)
    }

    private fun processClass(
        classes: MutableMap<Name, CirClassNode>,
        targetIndex: Int,
        classDescriptor: ClassDescriptor,
        parentCommonDeclaration: NullableLazyValue<*>?
    ) {
        val classNode: CirClassNode = classes.getOrPut(classDescriptor.name.intern()) {
            buildClassNode(storageManager, size, cacheRW, parentCommonDeclaration, classDescriptor.fqNameSafe.intern())
        }
        classNode.targetDeclarations[targetIndex] = CirClassFactory.create(classDescriptor)

        val parentCommonDeclarationForMembers: NullableLazyValue<CirClass> = classNode.commonDeclaration

        val constructors: MutableMap<ConstructorApproximationKey, CirClassConstructorNode> = classNode.constructors
        val properties: MutableMap<PropertyApproximationKey, CirPropertyNode> = classNode.properties
        val functions: MutableMap<FunctionApproximationKey, CirFunctionNode> = classNode.functions
        val nestedClasses: MutableMap<Name, CirClassNode> = classNode.classes

        classDescriptor.constructors.forEach { processClassConstructor(constructors, targetIndex, it, parentCommonDeclarationForMembers) }

        classDescriptor.unsubstitutedMemberScope.collectMembers(
            PropertyCollector { processProperty(properties, targetIndex, it, parentCommonDeclarationForMembers) },
            FunctionCollector { processFunction(functions, targetIndex, it, parentCommonDeclarationForMembers) },
            ClassCollector { processClass(nestedClasses, targetIndex, it, parentCommonDeclarationForMembers) }
        )
    }

    private fun processClassConstructor(
        constructors: MutableMap<ConstructorApproximationKey, CirClassConstructorNode>,
        targetIndex: Int,
        constructorDescriptor: ClassConstructorDescriptor,
        parentCommonDeclaration: NullableLazyValue<*>?
    ) {
        val constructorNode: CirClassConstructorNode = constructors.getOrPut(ConstructorApproximationKey(constructorDescriptor)) {
            buildClassConstructorNode(storageManager, size, cacheRW, parentCommonDeclaration)
        }
        constructorNode.targetDeclarations[targetIndex] = CirClassConstructorFactory.create(constructorDescriptor)
    }

    private fun processTypeAlias(
        typeAliases: MutableMap<Name, CirTypeAliasNode>,
        targetIndex: Int,
        typeAliasDescriptor: TypeAliasDescriptor
    ) {
        val typeAliasNode: CirTypeAliasNode = typeAliases.getOrPut(typeAliasDescriptor.name.intern()) {
            buildTypeAliasNode(storageManager, size, cacheRW, typeAliasDescriptor.fqNameSafe.intern())
        }
        typeAliasNode.targetDeclarations[targetIndex] = CirTypeAliasFactory.create(typeAliasDescriptor)
    }
}
