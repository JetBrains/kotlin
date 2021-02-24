/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.commonizer.*
import org.jetbrains.kotlin.descriptors.commonizer.ModulesProvider.ModuleInfo
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirEntityId
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirName
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirPackageName
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.*
import org.jetbrains.kotlin.resolve.scopes.MemberScope
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

    private val size = parameters.targetProviders.size

    fun merge(): CirTreeMergeResult {
        val result = processRoot()
        System.gc()
        return result
    }

    private fun processRoot(): CirTreeMergeResult {
        val rootNode: CirRootNode = buildRootNode(storageManager, size)

        // remember any exported forward declarations from common fragments of dependency modules
        parameters.dependencyModulesProvider?.loadModuleInfos()?.forEach(::processCInteropModuleAttributes)

        // load common dependencies
        val dependencyModules = parameters.dependencyModulesProvider?.loadModules(emptyList())?.values.orEmpty()

        val allModuleInfos: List<Map<String, ModuleInfo>> = parameters.targetProviders.map { targetProvider ->
            targetProvider.modulesProvider.loadModuleInfos().associateBy { it.name }
        }
        val commonModuleNames = parameters.getCommonModuleNames()

        parameters.targetProviders.forEachIndexed { targetIndex, targetProvider ->
            val commonModuleInfos = allModuleInfos[targetIndex].filterKeys { it in commonModuleNames }
            processTarget(rootNode, targetIndex, targetProvider, commonModuleInfos, dependencyModules)
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
        dependencyModules: Collection<ModuleDescriptor>
    ) {
        rootNode.targetDeclarations[targetIndex] = CirRootFactory.create(targetProvider.target)

        val targetDependencyModules = targetProvider.dependencyModulesProvider?.loadModules(dependencyModules)?.values.orEmpty()
        val allDependencyModules = targetDependencyModules + dependencyModules

        val moduleDescriptors: Map<String, ModuleDescriptor> = targetProvider.modulesProvider.loadModules(allDependencyModules)

        moduleDescriptors.forEach { (name, moduleDescriptor) ->
            val moduleInfo = commonModuleInfos[name] ?: return@forEach
            processModule(rootNode, targetIndex, moduleInfo, moduleDescriptor)
        }
    }

    private fun processModule(
        rootNode: CirRootNode,
        targetIndex: Int,
        moduleInfo: ModuleInfo,
        moduleDescriptor: ModuleDescriptor
    ) {
        processCInteropModuleAttributes(moduleInfo)

        val moduleName: CirName = CirName.create(moduleDescriptor.name)
        val moduleNode: CirModuleNode = rootNode.modules.getOrPut(moduleName) {
            buildModuleNode(storageManager, size)
        }
        moduleNode.targetDeclarations[targetIndex] = CirModuleFactory.create(moduleName)

        moduleDescriptor.collectNonEmptyPackageMemberScopes { packageName, packageMemberScope ->
            processPackage(moduleNode, targetIndex, packageName, packageMemberScope)
        }
    }

    private fun processPackage(
        moduleNode: CirModuleNode,
        targetIndex: Int,
        packageName: CirPackageName,
        packageMemberScope: MemberScope
    ) {
        val packageNode: CirPackageNode = moduleNode.packages.getOrPut(packageName) {
            buildPackageNode(storageManager, size)
        }
        packageNode.targetDeclarations[targetIndex] = CirPackageFactory.create(packageName)

        packageMemberScope.collectMembers(
            PropertyCollector { propertyDescriptor ->
                processProperty(packageNode, targetIndex, propertyDescriptor)
            },
            FunctionCollector { functionDescriptor ->
                processFunction(packageNode, targetIndex, functionDescriptor)
            },
            ClassCollector { classDescriptor ->
                processClass(packageNode, targetIndex, classDescriptor) { className ->
                    CirEntityId.create(packageName, className)
                }
            },
            TypeAliasCollector { typeAliasDescriptor ->
                processTypeAlias(packageNode, targetIndex, typeAliasDescriptor)
            }
        )
    }

    private fun processProperty(
        ownerNode: CirNodeWithMembers<*, *>,
        targetIndex: Int,
        propertyDescriptor: PropertyDescriptor
    ) {
        val propertyNode: CirPropertyNode = ownerNode.properties.getOrPut(PropertyApproximationKey(propertyDescriptor)) {
            buildPropertyNode(storageManager, size, classifiers, (ownerNode as? CirClassNode)?.commonDeclaration)
        }
        propertyNode.targetDeclarations[targetIndex] = CirPropertyFactory.create(
            source = propertyDescriptor,
            containingClass = (ownerNode as? CirClassNode)?.targetDeclarations?.get(targetIndex)
        )
    }

    private fun processFunction(
        ownerNode: CirNodeWithMembers<*, *>,
        targetIndex: Int,
        functionDescriptor: SimpleFunctionDescriptor
    ) {
        val functionNode: CirFunctionNode = ownerNode.functions.getOrPut(FunctionApproximationKey(functionDescriptor)) {
            buildFunctionNode(storageManager, size, classifiers, (ownerNode as? CirClassNode)?.commonDeclaration)
        }
        functionNode.targetDeclarations[targetIndex] = CirFunctionFactory.create(
            source = functionDescriptor,
            containingClass = (ownerNode as? CirClassNode)?.targetDeclarations?.get(targetIndex)
        )
    }

    private fun processClass(
        ownerNode: CirNodeWithMembers<*, *>,
        targetIndex: Int,
        classDescriptor: ClassDescriptor,
        classIdFunction: (CirName) -> CirEntityId
    ) {
        val className = CirName.create(classDescriptor.name)
        val classId = classIdFunction(className)

        val classNode: CirClassNode = ownerNode.classes.getOrPut(className) {
            buildClassNode(storageManager, size, classifiers, (ownerNode as? CirClassNode)?.commonDeclaration, classId)
        }
        classNode.targetDeclarations[targetIndex] = CirClassFactory.create(classDescriptor)

        if (classDescriptor.kind != ClassKind.ENUM_ENTRY) {
            classDescriptor.constructors.forEach { constructorDescriptor ->
                processClassConstructor(classNode, targetIndex, constructorDescriptor)
            }
        }

        classDescriptor.unsubstitutedMemberScope.collectMembers(
            PropertyCollector { propertyDescriptor ->
                processProperty(classNode, targetIndex, propertyDescriptor)
            },
            FunctionCollector { functionDescriptor ->
                processFunction(classNode, targetIndex, functionDescriptor)
            },
            ClassCollector { nestedClassDescriptor ->
                processClass(classNode, targetIndex, nestedClassDescriptor) { nestedClassName ->
                    classId.createNestedEntityId(nestedClassName)
                }
            }
        )
    }

    private fun processClassConstructor(
        classNode: CirClassNode,
        targetIndex: Int,
        constructorDescriptor: ClassConstructorDescriptor
    ) {
        val constructorNode: CirClassConstructorNode = classNode.constructors.getOrPut(ConstructorApproximationKey(constructorDescriptor)) {
            buildClassConstructorNode(storageManager, size, classifiers, classNode.commonDeclaration)
        }
        constructorNode.targetDeclarations[targetIndex] = CirClassConstructorFactory.create(
            source = constructorDescriptor,
            containingClass = classNode.targetDeclarations[targetIndex]!!
        )
    }

    private fun processTypeAlias(
        packageNode: CirPackageNode,
        targetIndex: Int,
        typeAliasDescriptor: TypeAliasDescriptor
    ) {
        val typeAliasName = CirName.create(typeAliasDescriptor.name)
        val typeAliasClassId = CirEntityId.create(packageNode.packageName, typeAliasName)

        val typeAliasNode: CirTypeAliasNode = packageNode.typeAliases.getOrPut(typeAliasName) {
            buildTypeAliasNode(storageManager, size, classifiers, typeAliasClassId)
        }
        typeAliasNode.targetDeclarations[targetIndex] = CirTypeAliasFactory.create(typeAliasDescriptor)
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
