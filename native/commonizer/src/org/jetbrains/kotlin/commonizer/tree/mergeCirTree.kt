/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.tree

import org.jetbrains.kotlin.commonizer.TargetDependent
import org.jetbrains.kotlin.commonizer.cir.*
import org.jetbrains.kotlin.commonizer.CommonizerSettings
import org.jetbrains.kotlin.commonizer.mergedtree.*
import org.jetbrains.kotlin.commonizer.mergedtree.CirNodeRelationship.Companion.ParentNode
import org.jetbrains.kotlin.commonizer.mergedtree.CirNodeRelationship.ParentNode
import org.jetbrains.kotlin.commonizer.utils.fastForEach
import org.jetbrains.kotlin.storage.StorageManager

internal data class TargetBuildingContext(
    val storageManager: StorageManager,
    val classifiers: CirKnownClassifiers,
    val memberContext: CirMemberContext = CirMemberContext.empty,
    val commonizerSettings: CommonizerSettings,
    val targets: Int, val targetIndex: Int
) {
    fun withMemberContextOf(clazz: CirClass) = copy(memberContext = memberContext.withContextOf(clazz))
}

internal fun mergeCirTree(
    storageManager: StorageManager,
    classifiers: CirKnownClassifiers,
    roots: TargetDependent<CirTreeRoot>,
    settings: CommonizerSettings,
): CirRootNode {
    val node = buildRootNode(storageManager, classifiers.commonDependencies, roots.size)
    roots.targets.withIndex().forEach { (targetIndex, target) ->
        node.targetDeclarations[targetIndex] = CirRoot.create(target)
        node.buildModules(
            TargetBuildingContext(
                storageManager = storageManager,
                classifiers = classifiers,
                memberContext = CirMemberContext.empty,
                commonizerSettings = settings,
                targets = roots.size,
                targetIndex = targetIndex
            ), roots[target].modules
        )
    }
    return node
}

internal fun CirRootNode.buildModules(context: TargetBuildingContext, modules: List<CirTreeModule>) {
    modules.fastForEach { module -> buildModule(context, module) }
}

internal fun CirRootNode.buildModule(context: TargetBuildingContext, treeModule: CirTreeModule) {
    val moduleNode = modules.getOrPut(treeModule.module.name) {
        buildModuleNode(context.storageManager, context.targets)
    }
    moduleNode.targetDeclarations[context.targetIndex] = treeModule.module
    treeModule.packages.fastForEach { pkg -> moduleNode.buildPackage(context, pkg) }
}

internal fun CirModuleNode.buildPackage(context: TargetBuildingContext, treePackage: CirTreePackage) {
    val packageNode = packages.getOrPut(treePackage.pkg.packageName) {
        buildPackageNode(context.storageManager, context.targets)
    }
    packageNode.targetDeclarations[context.targetIndex] = treePackage.pkg
    treePackage.functions.fastForEach { function -> packageNode.buildFunction(context, function) }
    treePackage.properties.fastForEach { property -> packageNode.buildProperty(context, property) }
    treePackage.typeAliases.fastForEach { typeAlias -> packageNode.buildTypeAlias(context, typeAlias) }
    treePackage.classes.fastForEach { clazz -> packageNode.buildClass(context, clazz) }
}

internal fun CirNodeWithMembers<*, *>.buildClass(
    context: TargetBuildingContext, treeClass: CirTreeClass, parent: CirNode<*, *>? = null
) {
    val classNode = classes.getOrPut(treeClass.clazz.name) {
        buildClassNode(
            context.storageManager,
            context.targets,
            context.classifiers,
            context.commonizerSettings,
            ParentNode(parent),
            treeClass.id,
        )
    }
    classNode.targetDeclarations[context.targetIndex] = treeClass.clazz
    val contextWithClass = context.withMemberContextOf(treeClass.clazz)
    treeClass.functions.fastForEach { function -> classNode.buildFunction(contextWithClass, function, classNode) }
    treeClass.properties.fastForEach { property -> classNode.buildProperty(contextWithClass, property, classNode) }
    treeClass.constructors.fastForEach { constructor -> classNode.buildConstructor(contextWithClass, constructor, classNode) }
    treeClass.classes.fastForEach { clazz -> classNode.buildClass(contextWithClass, clazz, classNode) }
}

internal fun CirNodeWithMembers<*, *>.buildFunction(
    context: TargetBuildingContext, function: CirFunction, parent: CirNode<*, *>? = null
) {
    val functionNode = functions.getOrPut(
        FunctionApproximationKey.create(function, SignatureBuildingContext(context.memberContext, function))
    ) {
        buildFunctionNode(context.storageManager, context.targets, context.classifiers, context.commonizerSettings, ParentNode(parent))
    }
    /* Multiple type substitutions could in result in the same commonization result */
    functionNode.targetDeclarations.set(context.targetIndex, function)
}

internal fun CirNodeWithMembers<*, *>.buildProperty(
    context: TargetBuildingContext, property: CirProperty, parent: CirNode<*, *>? = null
) {
    val propertyNode = properties.getOrPut(
        PropertyApproximationKey.create(property, SignatureBuildingContext(context.memberContext, property))
    ) {
        buildPropertyNode(context.storageManager, context.targets, context.classifiers, context.commonizerSettings, ParentNode(parent))
    }
    /* Multiple type substitutions could in result in the same commonization result */
    propertyNode.targetDeclarations.set(context.targetIndex, property)
}

internal fun CirClassNode.buildConstructor(
    context: TargetBuildingContext, constructor: CirClassConstructor, parent: CirNode<*, *>
) {
    val constructorNode = constructors.getOrPut(
        ConstructorApproximationKey.create(constructor, SignatureBuildingContext(context.memberContext, constructor))
    ) {
        buildClassConstructorNode(
            context.storageManager,
            context.targets,
            context.classifiers,
            context.commonizerSettings,
            ParentNode(parent),
        )
    }
    /* Multiple type substitutions could in result in the same commonization result */
    constructorNode.targetDeclarations.set(context.targetIndex, constructor)
}

internal fun CirPackageNode.buildTypeAlias(context: TargetBuildingContext, treeTypeAlias: CirTreeTypeAlias) {
    val typeAliasNode = typeAliases.getOrPut(treeTypeAlias.typeAlias.name) {
        buildTypeAliasNode(context.storageManager, context.targets, context.classifiers, context.commonizerSettings, treeTypeAlias.id)
    }
    typeAliasNode.targetDeclarations[context.targetIndex] = treeTypeAlias.typeAlias
}
