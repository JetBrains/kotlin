/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.transformer

import org.jetbrains.kotlin.commonizer.mergedtree.*
import org.jetbrains.kotlin.commonizer.mergedtree.CirNodeRelationship.ParentNode
import org.jetbrains.kotlin.storage.StorageManager

internal class TypeSubstitutionCirNodeTransformer(
    private val storageManager: StorageManager,
    private val classifiers: CirKnownClassifiers,
    private val typeSubstitutor: CirTypeSubstitutor
) : CirNodeTransformer {

    override fun invoke(root: CirRootNode) {
        for (index in 0 until root.targetDeclarations.size) {
            root.modules.forEach { (_, module) -> this(module, index) }
        }
    }

    private operator fun invoke(module: CirModuleNode, index: Int) {
        module.packages.forEach { (_, pkg) -> this(pkg, index) }
    }

    private operator fun invoke(pkg: CirPackageNode, index: Int) {
        pkg.functions.values.toList().forEach { function -> this(pkg, function, index, CirMemberContext.empty) }
        pkg.properties.values.toList().forEach { property -> this(pkg, property, index, CirMemberContext.empty) }
        pkg.classes.values.toList().forEach { clazz -> this(clazz, index, CirMemberContext.empty) }
    }

    private operator fun invoke(clazz: CirClassNode, index: Int, context: CirMemberContext) {
        val contextWithClass = context.withContextOf(clazz.targetDeclarations[index] ?: return)
        clazz.functions.values.toList().forEach { function -> this(clazz, function, index, contextWithClass) }
        clazz.properties.values.toList().forEach { property -> this(clazz, property, index, contextWithClass) }
        clazz.classes.values.toList().forEach { innerClass -> this(innerClass, index, contextWithClass) }
    }

    private operator fun invoke(parent: CirNodeWithMembers<*, *>, function: CirFunctionNode, index: Int, context: CirMemberContext) {
        /* Only perform type substitution to nodes that are not 'complete' */
        if (function.targetDeclarations.none { it == null }) return
        val originalFunction = function.targetDeclarations[index] ?: return
        val newFunction = typeSubstitutor.substitute(index, originalFunction)
        if (originalFunction == newFunction) return

        val approximationKey = FunctionApproximationKey.create(context, newFunction)
        val newNode = parent.functions.getOrPut(approximationKey) {
            buildFunctionNode(storageManager, parent.targetDeclarations.size, classifiers, ParentNode(parent))
        }

        newNode.targetDeclarations.setAllowingOverride(index, newFunction)
    }

    private operator fun invoke(parent: CirNodeWithMembers<*, *>, property: CirPropertyNode, index: Int, context: CirMemberContext) {
        val originalProperty = property.targetDeclarations[index] ?: return
        val newProperty = typeSubstitutor.substitute(index, originalProperty)
        if (originalProperty == newProperty) return

        val approximationKey = PropertyApproximationKey.create(context, newProperty)
        val newNode = parent.properties.getOrPut(approximationKey) {
            buildPropertyNode(storageManager, parent.targetDeclarations.size, classifiers, ParentNode(parent))
        }

        newNode.targetDeclarations.setAllowingOverride(index, newProperty)
    }
}