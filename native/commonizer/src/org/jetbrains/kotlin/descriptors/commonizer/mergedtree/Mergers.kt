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


data class TargetMergeContext(
    val storageManager: StorageManager,
    val classifiers: CirKnownClassifiers,
    val parameters: CommonizerParameters,
    val targetIndex: Int
) {
    val targets = parameters.targetProviders.size
}

class RootMerger(
    private val targetMerger: TargetMerger
) {
    fun processRoot(
        storageManager: StorageManager,
        classifiers: CirKnownClassifiers,
        parameters: CommonizerParameters
    ): CirTreeMerger.CirTreeMergeResult {
        val rootNode: CirRootNode = buildRootNode(storageManager, parameters.targetProviders.size)

        // remember any exported forward declarations from common fragments of dependee modules
        parameters.dependeeModulesProvider?.loadModuleInfos()?.values?.forEach { processCInteropModuleAttributes(classifiers, it) }

        // load common dependencies
        val dependeeModules = parameters.dependeeModulesProvider?.loadModules(emptyList())?.values.orEmpty()

        val allModuleInfos: List<Map<String, ModulesProvider.ModuleInfo>> =
            parameters.targetProviders.map { it.modulesProvider.loadModuleInfos() }
        val commonModuleNames = allModuleInfos.map { it.keys }.reduce { a, b -> a intersect b }

        parameters.targetProviders.forEachIndexed { targetIndex, targetProvider ->
            val commonModuleInfos = allModuleInfos[targetIndex].filterKeys { it in commonModuleNames }
            targetMerger.processTarget(
                TargetMergeContext(
                    storageManager, classifiers, parameters, targetIndex
                ), rootNode, targetProvider, commonModuleInfos, dependeeModules
            )
            parameters.progressLogger?.invoke("Loaded declarations for [${targetProvider.target.name}]")
            System.gc()
        }

        val absentModuleInfos = allModuleInfos.mapIndexed { index, moduleInfos ->
            val target = parameters.targetProviders[index].target
            val absentInfos = moduleInfos.filterKeys { name -> name !in commonModuleNames }.values
            target to absentInfos
        }.toMap()

        return CirTreeMerger.CirTreeMergeResult(
            root = rootNode,
            missingModuleInfos = absentModuleInfos
        )
    }
}

class TargetMerger(
    private val moduleMerger: ModuleMerger
) {
    fun processTarget(
        context: TargetMergeContext,
        rootNode: CirRootNode,
        targetProvider: TargetProvider,
        commonModuleInfos: Map<String, ModulesProvider.ModuleInfo>,
        dependeeModules: Collection<ModuleDescriptor>
    ) {
        rootNode.targetDeclarations[context.targetIndex] = CirRootFactory.create(
            targetProvider.target,
            targetProvider.builtInsClass.name,
            targetProvider.builtInsProvider
        )

        val targetDependeeModules = targetProvider.dependeeModulesProvider?.loadModules(dependeeModules)?.values.orEmpty()
        val allDependeeModules = targetDependeeModules + dependeeModules

        val moduleDescriptors: Map<String, ModuleDescriptor> = targetProvider.modulesProvider.loadModules(allDependeeModules)
        val modules: MutableMap<Name, CirModuleNode> = rootNode.modules

        moduleDescriptors.forEach { (name, moduleDescriptor) ->
            val moduleInfo = commonModuleInfos[name] ?: return@forEach
            moduleMerger.processModule(context, modules, moduleInfo, moduleDescriptor)
        }
    }
}

class ModuleMerger(
    private val packageMerger: PackageMerger
) {
    fun processModule(
        context: TargetMergeContext,
        modules: MutableMap<Name, CirModuleNode>,
        moduleInfo: ModulesProvider.ModuleInfo,
        moduleDescriptor: ModuleDescriptor
    ) {
        processCInteropModuleAttributes(context.classifiers, moduleInfo)

        val moduleName: Name = moduleDescriptor.name.intern()
        val moduleNode: CirModuleNode = modules.getOrPut(moduleName) {
            buildModuleNode(context.storageManager, context.targets)
        }
        moduleNode.targetDeclarations[context.targetIndex] = CirModuleFactory.create(moduleDescriptor)

        val packages: MutableMap<FqName, CirPackageNode> = moduleNode.packages

        moduleDescriptor.collectNonEmptyPackageMemberScopes { packageFqName, packageMemberScope ->
            packageMerger.processPackage(context, packages, packageFqName.intern(), packageMemberScope, moduleName)
        }
    }
}

class PackageMerger(
    private val propertyMerger: PropertyMerger?,
    private val functionMerger: FunctionMerger?,
    private val classMerger: ClassMerger?,
    private val typeAliasMerger: TypeAliasMerger?
) {
    fun processPackage(
        context: TargetMergeContext,
        packages: MutableMap<FqName, CirPackageNode>,
        packageFqName: FqName,
        packageMemberScope: MemberScope,
        moduleName: Name
    ) {
        val packageNode: CirPackageNode = packages.getOrPut(packageFqName) {
            buildPackageNode(context.storageManager, context.targets, packageFqName, moduleName)
        }
        packageNode.targetDeclarations[context.targetIndex] = CirPackageFactory.create(packageFqName)

        val properties: MutableMap<PropertyApproximationKey, CirPropertyNode> = packageNode.properties
        val functions: MutableMap<FunctionApproximationKey, CirFunctionNode> = packageNode.functions
        val classes: MutableMap<Name, CirClassNode> = packageNode.classes
        val typeAliases: MutableMap<Name, CirTypeAliasNode> = packageNode.typeAliases

        packageMemberScope.collectMembers(
            PropertyCollector { propertyDescriptor ->
                propertyMerger?.processProperty(context, properties, propertyDescriptor, null)
            },
            FunctionCollector { functionDescriptor ->
                functionMerger?.processFunction(context, functions, functionDescriptor, null)
            },
            ClassCollector { classDescriptor ->
                classMerger?.processClass(context, classes, classDescriptor, null) { className ->
                    internedClassId(packageFqName, className)
                }
            },
            TypeAliasCollector { typeAliasDescriptor ->
                typeAliasMerger?.processTypeAlias(context, typeAliases, typeAliasDescriptor, packageFqName)
            }
        )
    }
}

object PropertyMerger {
    fun processProperty(
        context: TargetMergeContext,
        properties: MutableMap<PropertyApproximationKey, CirPropertyNode>,
        propertyDescriptor: PropertyDescriptor,
        parentCommonDeclaration: NullableLazyValue<*>?
    ) = with(context) {
        val propertyNode: CirPropertyNode = properties.getOrPut(PropertyApproximationKey(propertyDescriptor)) {
            buildPropertyNode(storageManager, targets, classifiers, parentCommonDeclaration)
        }
        propertyNode.targetDeclarations[targetIndex] = CirPropertyFactory.create(propertyDescriptor)
    }
}

object FunctionMerger {
    fun processFunction(
        context: TargetMergeContext,
        functions: MutableMap<FunctionApproximationKey, CirFunctionNode>,
        functionDescriptor: SimpleFunctionDescriptor,
        parentCommonDeclaration: NullableLazyValue<*>?
    ) = with(context) {
        val functionNode: CirFunctionNode = functions.getOrPut(FunctionApproximationKey(functionDescriptor)) {
            buildFunctionNode(storageManager, targets, classifiers, parentCommonDeclaration)
        }
        functionNode.targetDeclarations[targetIndex] = CirFunctionFactory.create(functionDescriptor)
    }
}

class ClassMerger(
    private val classConstructorMerger: ClassConstructorMerger?,
    private val propertyMerger: PropertyMerger?,
    private val functionMerger: FunctionMerger?
) {
    fun processClass(
        context: TargetMergeContext,
        classes: MutableMap<Name, CirClassNode>,
        classDescriptor: ClassDescriptor,
        parentCommonDeclaration: NullableLazyValue<*>?,
        classIdFunction: (Name) -> ClassId
    ): Unit = with(context) {
        val className = classDescriptor.name.intern()
        val classId = classIdFunction(className)

        val classNode: CirClassNode = classes.getOrPut(className) {
            buildClassNode(storageManager, targets, classifiers, parentCommonDeclaration, classId)
        }
        classNode.targetDeclarations[targetIndex] = CirClassFactory.create(classDescriptor)

        val parentCommonDeclarationForMembers: NullableLazyValue<CirClass> = classNode.commonDeclaration

        val constructors: MutableMap<ConstructorApproximationKey, CirClassConstructorNode> = classNode.constructors
        val properties: MutableMap<PropertyApproximationKey, CirPropertyNode> = classNode.properties
        val functions: MutableMap<FunctionApproximationKey, CirFunctionNode> = classNode.functions
        val nestedClasses: MutableMap<Name, CirClassNode> = classNode.classes

        classDescriptor.constructors.forEach {
            classConstructorMerger?.processClassConstructor(context, constructors, it, parentCommonDeclarationForMembers)
        }

        classDescriptor.unsubstitutedMemberScope.collectMembers(
            PropertyCollector { propertyDescriptor ->
                propertyMerger?.processProperty(context, properties, propertyDescriptor, parentCommonDeclarationForMembers)
            },
            FunctionCollector { functionDescriptor ->
                functionMerger?.processFunction(context, functions, functionDescriptor, parentCommonDeclarationForMembers)
            },
            ClassCollector { nestedClassDescriptor ->
                processClass(context, nestedClasses, nestedClassDescriptor, parentCommonDeclarationForMembers) { nestedClassName ->
                    internedClassId(classId, nestedClassName)
                }
            }
        )
    }
}

object ClassConstructorMerger {
    fun processClassConstructor(
        context: TargetMergeContext,
        constructors: MutableMap<ConstructorApproximationKey, CirClassConstructorNode>,
        constructorDescriptor: ClassConstructorDescriptor,
        parentCommonDeclaration: NullableLazyValue<*>?
    ) = with(context) {
        val constructorNode: CirClassConstructorNode = constructors.getOrPut(ConstructorApproximationKey(constructorDescriptor)) {
            buildClassConstructorNode(storageManager, targets, classifiers, parentCommonDeclaration)
        }
        constructorNode.targetDeclarations[targetIndex] = CirClassConstructorFactory.create(constructorDescriptor)
    }
}

object TypeAliasMerger {
    fun processTypeAlias(
        context: TargetMergeContext,
        typeAliases: MutableMap<Name, CirTypeAliasNode>,
        typeAliasDescriptor: TypeAliasDescriptor,
        packageFqName: FqName
    ) = with(context) {
        val typeAliasName = typeAliasDescriptor.name.intern()
        val typeAliasClassId = internedClassId(packageFqName, typeAliasName)

        val typeAliasNode: CirTypeAliasNode = typeAliases.getOrPut(typeAliasName) {
            buildTypeAliasNode(storageManager, targets, classifiers, typeAliasClassId)
        }
        typeAliasNode.targetDeclarations[targetIndex] = CirTypeAliasFactory.create(typeAliasDescriptor)
    }
}

internal fun processCInteropModuleAttributes(classifiers: CirKnownClassifiers, moduleInfo: ModulesProvider.ModuleInfo) {
    val cInteropAttributes = moduleInfo.cInteropAttributes ?: return
    val exportForwardDeclarations = cInteropAttributes.exportForwardDeclarations.takeIf { it.isNotEmpty() } ?: return
    val mainPackageFqName = FqName(cInteropAttributes.mainPackageFqName).intern()

    exportForwardDeclarations.forEach { classFqName ->
        // Class has synthetic package FQ name (cnames/objcnames). Need to transfer it to the main package.
        val className = Name.identifier(classFqName.substringAfterLast('.')).intern()
        classifiers.forwardDeclarations.addExportedForwardDeclaration(internedClassId(mainPackageFqName, className))
    }
}
