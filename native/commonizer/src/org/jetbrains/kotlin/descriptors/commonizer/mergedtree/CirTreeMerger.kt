/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.commonizer.LeafTarget
import org.jetbrains.kotlin.descriptors.commonizer.ModulesProvider.ModuleInfo
import org.jetbrains.kotlin.descriptors.commonizer.CommonizerParameters
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
        val missingModuleInfos: Map<LeafTarget, Collection<ModuleInfo>>
    )

    private val size = parameters.targetProviders.size

    fun merge(): CirTreeMergeResult {
        val result = processRoot()
        System.gc()
        return result
    }

    private fun processRoot(): CirTreeMergeResult {
        val rootNode: CirRootNode = buildRootNode(storageManager, size)

        // remember any exported forward declarations from common fragments of dependee modules
        parameters.dependeeModulesProvider?.loadModuleInfos()?.values?.forEach(::processCInteropModuleAttributes)

        // load common dependencies
        val dependeeModules = parameters.dependeeModulesProvider?.loadModules(emptyList())?.values.orEmpty()

        val allModuleInfos: List<Map<String, ModuleInfo>> = parameters.targetProviders.map { it.modulesProvider.loadModuleInfos() }
        val commonModuleNames = allModuleInfos.map { it.keys }.reduce { a, b -> a intersect b }

        parameters.targetProviders.forEachIndexed { targetIndex, targetProvider ->
            val commonModuleInfos = allModuleInfos[targetIndex].filterKeys { it in commonModuleNames }
            processTarget(rootNode, targetIndex, targetProvider, commonModuleInfos, dependeeModules)
            parameters.progressLogger?.invoke("Loaded declarations for ${targetProvider.target.prettyName}")
            System.gc()
        }

        val missingModuleInfos = allModuleInfos.mapIndexed { index, moduleInfos ->
            val target = parameters.targetProviders[index].target
            val missingInfos = moduleInfos.filterKeys { name -> name !in commonModuleNames }.values
            target to missingInfos
        }.toMap()

        return CirTreeMergeResult(
            root = rootNode,
            missingModuleInfos = missingModuleInfos
        )
    }

    private fun processTarget(
        rootNode: CirRootNode,
        targetIndex: Int,
        targetProvider: TargetProvider,
        commonModuleInfos: Map<String, ModuleInfo>,
        dependeeModules: Collection<ModuleDescriptor>
    ) {
        rootNode.targetDeclarations[targetIndex] = CirRootFactory.create(
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
            processModule(modules, targetIndex, moduleInfo, moduleDescriptor)
        }
    }

    private fun processModule(
        modules: MutableMap<Name, CirModuleNode>,
        targetIndex: Int,
        moduleInfo: ModuleInfo,
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

    private fun processCInteropModuleAttributes(moduleInfo: ModuleInfo) {
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
