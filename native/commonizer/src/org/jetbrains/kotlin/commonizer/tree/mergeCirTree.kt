/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.tree

import org.jetbrains.kotlin.commonizer.TargetDependent
import org.jetbrains.kotlin.commonizer.cir.AliasedCirClassImpl
import org.jetbrains.kotlin.commonizer.cir.CirClass
import org.jetbrains.kotlin.commonizer.cir.CirEntityId
import org.jetbrains.kotlin.commonizer.cir.CirRoot
import org.jetbrains.kotlin.commonizer.mergedtree.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.storage.StorageManager

internal data class TargetBuildingContext(
    val storageManager: StorageManager, val classifiers: CirKnownClassifiers, val targets: Int, val targetIndex: Int
)

internal fun mergeCirTree(
    storageManager: StorageManager, classifiers: CirKnownClassifiers, roots: TargetDependent<CirTreeRoot>
): CirRootNode {
    val node = buildRootNode(storageManager, roots.size)
    roots.targets.withIndex().forEach { (targetIndex, target) ->
        node.targetDeclarations[targetIndex] = CirRoot.create(target)
        node.buildModules(TargetBuildingContext(storageManager, classifiers, roots.size, targetIndex), roots[target].modules)
    }
    node.buildTypeAliasClassStubs()
    return node
}

internal fun CirRootNode.buildModules(context: TargetBuildingContext, modules: List<CirTreeModule>) {
    modules.forEach { module -> buildModule(context, module) }
}

internal fun CirRootNode.buildModule(context: TargetBuildingContext, treeModule: CirTreeModule) {
    val moduleNode = modules.getOrPut(treeModule.module.name) {
        buildModuleNode(context.storageManager, context.targets)
    }
    moduleNode.targetDeclarations[context.targetIndex] = treeModule.module
    treeModule.packages.forEach { pkg -> moduleNode.buildPackage(context, pkg) }
}

internal fun CirModuleNode.buildPackage(context: TargetBuildingContext, treePackage: CirTreePackage) {
    val packageNode = packages.getOrPut(treePackage.pkg.packageName) {
        buildPackageNode(context.storageManager, context.targets)
    }
    packageNode.targetDeclarations[context.targetIndex] = treePackage.pkg
    treePackage.functions.forEach { function -> packageNode.buildFunction(context, function) }
    treePackage.properties.forEach { property -> packageNode.buildProperty(context, property) }
    treePackage.typeAliases.forEach { typeAlias -> packageNode.buildTypeAlias(context, typeAlias) }
    treePackage.classes.forEach { clazz -> packageNode.buildClass(context, clazz) }
}

internal fun CirNodeWithMembers<*, *>.buildClass(
    context: TargetBuildingContext, treeClass: CirTreeClass, parent: CirNode<*, *>? = null
) {
    val classNode = classes.getOrPut(treeClass.clazz.name) {
        buildClassNode(context.storageManager, context.targets, context.classifiers, parent?.commonDeclaration, treeClass.id)
    }
    classNode.targetDeclarations[context.targetIndex] = treeClass.clazz
    treeClass.functions.forEach { function -> classNode.buildFunction(context, function, classNode) }
    treeClass.properties.forEach { property -> classNode.buildProperty(context, property, classNode) }
    treeClass.constructors.forEach { constructor -> classNode.buildConstructor(context, constructor, classNode) }
    treeClass.classes.forEach { clazz -> classNode.buildClass(context, clazz, classNode) }
}

internal fun CirNodeWithMembers<*, *>.buildFunction(
    context: TargetBuildingContext, treeFunction: CirTreeFunction, parent: CirNode<*, *>? = null
) {
    val functionNode = functions.getOrPut(treeFunction.approximationKey) {
        buildFunctionNode(context.storageManager, context.targets, context.classifiers, parent?.commonDeclaration)
    }
    functionNode.targetDeclarations[context.targetIndex] = treeFunction.function
}

internal fun CirNodeWithMembers<*, *>.buildProperty(
    context: TargetBuildingContext, treeProperty: CirTreeProperty, parent: CirNode<*, *>? = null
) {
    val propertyNode = properties.getOrPut(treeProperty.approximationKey) {
        buildPropertyNode(context.storageManager, context.targets, context.classifiers, parent?.commonDeclaration)
    }
    propertyNode.targetDeclarations[context.targetIndex] = treeProperty.property
}

internal fun CirClassNode.buildConstructor(
    context: TargetBuildingContext, treeConstructor: CirTreeClassConstructor, parent: CirNode<*, *>?
) {
    val constructorNode = constructors.getOrPut(treeConstructor.approximationKey) {
        buildClassConstructorNode(context.storageManager, context.targets, context.classifiers, parent?.commonDeclaration)
    }
    constructorNode.targetDeclarations[context.targetIndex] = treeConstructor.constructor
}

internal fun CirPackageNode.buildTypeAlias(context: TargetBuildingContext, treeTypeAlias: CirTreeTypeAlias) {
    val typeAliasNode = typeAliases.getOrPut(treeTypeAlias.typeAlias.name) {
        buildTypeAliasNode(context.storageManager, context.targets, context.classifiers, treeTypeAlias.id)
    }
    typeAliasNode.targetDeclarations[context.targetIndex] = treeTypeAlias.typeAlias
}

internal fun CirRootNode.buildTypeAliasClassStubs() {
    this.modules.values.forEach { moduleNode ->
        val classes: Map<CirEntityId, CirClassNode> = moduleNode.packages.flatMap { it.value.classes.values }.associateBy { it.id }

        moduleNode.packages.flatMap { it.value.typeAliases.values }
            .forEach { typeAliasNode ->
                for (index in typeAliasNode.targetDeclarations.indices) {
                    val typeAlias = typeAliasNode.targetDeclarations[index] ?: continue

                    val associatedClassNode = classes[typeAliasNode.id] ?: continue
                    val expandedClassNode = classes[typeAlias.expandedType.classifierId]
                    val expandedClass = expandedClassNode?.targetDeclarations?.get(index)
                        ?: CirClass.create(
                            emptyList(),
                            typeAlias.name,
                            typeParameters = emptyList(),
                            visibility = Visibilities.Public,
                            modality = Modality.FINAL,
                            ClassKind.CLASS,
                            null,
                            false,
                            false,
                            false,
                            false,
                            false
                        ).also {
                            it.supertypes = emptyList()
                        }

                    if (associatedClassNode.targetDeclarations[index] == null) {
                        val aliasedClass = AliasedCirClassImpl(
                            pointingTypeAlias = typeAlias,
                            pointedClass = expandedClass
                        )

                        associatedClassNode.targetDeclarations[index] = aliasedClass

                        expandedClassNode?.constructors?.forEach { (key, constructorNode) ->
                            associatedClassNode.constructors[key]?.targetDeclarations?.set(
                                index,
                                constructorNode.targetDeclarations[index]!!.withContainingClass(aliasedClass)
                            )
                        }

                        expandedClassNode?.properties?.forEach { (key, propertyNode) ->
                            associatedClassNode.properties[key]?.targetDeclarations?.set(
                                index,
                                propertyNode.targetDeclarations[index]!!.withContainingClass(aliasedClass)
                            )
                        }

                        expandedClassNode?.functions?.forEach { (key, functionNode) ->
                            associatedClassNode.functions[key]?.targetDeclarations?.set(
                                index,
                                functionNode.targetDeclarations[index]!!.withContainingClass(aliasedClass)
                            )
                        }
                    }
                }
            }
    }
}
