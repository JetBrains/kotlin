/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.transformer

import org.jetbrains.kotlin.commonizer.CommonizerSettings
import org.jetbrains.kotlin.commonizer.cir.CirHasTypeParameters
import org.jetbrains.kotlin.commonizer.mergedtree.*
import org.jetbrains.kotlin.commonizer.mergedtree.CirNodeRelationship.ParentNode
import org.jetbrains.kotlin.commonizer.mergedtree.ClassifierSignatureBuildingContext.TypeAliasInvariant
import org.jetbrains.kotlin.commonizer.utils.fastForEach
import org.jetbrains.kotlin.storage.StorageManager

internal class ReApproximationCirNodeTransformer(
    private val storageManager: StorageManager,
    private val classifiers: CirKnownClassifiers,
    private val settings: CommonizerSettings,
    private val signatureBuildingContextProvider: SignatureBuildingContextProvider,
) : CirNodeTransformer {

    internal class SignatureBuildingContextProvider(
        private val classifiers: CirKnownClassifiers,
        private val typeAliasInvariant: Boolean,
        private val skipArguments: Boolean
    ) {
        operator fun invoke(member: CirMemberContext, functionOrPropertyOrConstructor: CirHasTypeParameters): SignatureBuildingContext {
            return SignatureBuildingContext(
                memberContext = member, functionOrPropertyOrConstructor = functionOrPropertyOrConstructor,
                classifierSignatureBuildingContext = if (typeAliasInvariant) TypeAliasInvariant(classifiers.associatedIdsResolver)
                else ClassifierSignatureBuildingContext.Default,
                argumentsSignatureBuildingContext = if (skipArguments) ArgumentsSignatureBuildingContext.SkipArguments
                else ArgumentsSignatureBuildingContext.Default
            )
        }
    }

    override fun invoke(root: CirRootNode) {
        for (index in 0 until root.targetDeclarations.size) {
            root.modules.forEach { (_, module) -> this(module, index) }
        }
    }

    private operator fun invoke(module: CirModuleNode, index: Int) {
        module.packages.forEach { (_, pkg) -> this(pkg, index) }
    }

    private operator fun invoke(pkg: CirPackageNode, index: Int) {
        pkg.functions.values.toTypedArray().fastForEach { function -> this(pkg, function, index, CirMemberContext.empty) }
        pkg.properties.values.toTypedArray().fastForEach { property -> this(pkg, property, index, CirMemberContext.empty) }
        pkg.classes.values.toTypedArray().fastForEach { clazz -> this(clazz, index, CirMemberContext.empty) }
    }

    private operator fun invoke(clazz: CirClassNode, index: Int, context: CirMemberContext) {
        val contextWithClass = context.withContextOf(clazz.targetDeclarations[index] ?: return)
        clazz.functions.values.toTypedArray().fastForEach { function -> this(clazz, function, index, contextWithClass) }
        clazz.properties.values.toTypedArray().fastForEach { property -> this(clazz, property, index, contextWithClass) }
        clazz.constructors.values.toTypedArray().fastForEach { constructor -> this(clazz, constructor, index, contextWithClass) }
        clazz.classes.values.toTypedArray().fastForEach { innerClass -> this(innerClass, index, contextWithClass) }
    }

    private operator fun invoke(parent: CirNodeWithMembers<*, *>, function: CirFunctionNode, index: Int, context: CirMemberContext) {
        /* Only perform re-approximation to nodes that are not 'complete' */
        if (function.targetDeclarations.none { it == null }) return
        val functionAtIndex = function.targetDeclarations[index] ?: return

        val approximationKey = FunctionApproximationKey.create(functionAtIndex, signatureBuildingContextProvider(context, functionAtIndex))
        val newNode = parent.functions.getOrPut(approximationKey) {
            buildFunctionNode(storageManager, parent.targetDeclarations.size, classifiers, settings, ParentNode(parent))
        }

        // Move declaration
        if (newNode.targetDeclarations[index] == null) {
            function.targetDeclarations[index] = null
            newNode.targetDeclarations[index] = functionAtIndex
        }
    }

    private operator fun invoke(parent: CirNodeWithMembers<*, *>, property: CirPropertyNode, index: Int, context: CirMemberContext) {
        /* Only perform re-approximation to nodes that are not 'complete' */
        if (property.targetDeclarations.none { it == null }) return
        val propertyAtIndex = property.targetDeclarations[index] ?: return

        val approximationKey = PropertyApproximationKey.create(propertyAtIndex, signatureBuildingContextProvider(context, propertyAtIndex))
        val newNode = parent.properties.getOrPut(approximationKey) {
            buildPropertyNode(storageManager, parent.targetDeclarations.size, classifiers, settings, ParentNode(parent))
        }

        // Move declaration
        if (newNode.targetDeclarations[index] == null) {
            property.targetDeclarations[index] = null
            newNode.targetDeclarations[index] = propertyAtIndex
        }
    }

    private operator fun invoke(
        parent: CirClassNode, constructor: CirClassConstructorNode, index: Int, context: CirMemberContext
    ) {
        /* Only perform re-approximation to nodes that are not 'complete' */
        if (constructor.targetDeclarations.none { it == null }) return
        val constructorAtIndex = constructor.targetDeclarations[index] ?: return

        val approximationKey =
            ConstructorApproximationKey.create(constructorAtIndex, signatureBuildingContextProvider(context, constructorAtIndex))

        val newNode = parent.constructors.getOrPut(approximationKey) {
            buildClassConstructorNode(storageManager, parent.targetDeclarations.size, classifiers, settings, ParentNode(parent))
        }

        // Move declaration
        if (newNode.targetDeclarations[index] == null) {
            constructor.targetDeclarations[index] = null
            newNode.targetDeclarations[index] = constructorAtIndex
        }
    }
}
