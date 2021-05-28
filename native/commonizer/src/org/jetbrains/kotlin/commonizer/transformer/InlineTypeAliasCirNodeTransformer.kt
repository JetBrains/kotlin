/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.transformer

import org.jetbrains.kotlin.commonizer.cir.*
import org.jetbrains.kotlin.commonizer.mergedtree.CirClassNode
import org.jetbrains.kotlin.commonizer.mergedtree.CirModuleNode
import org.jetbrains.kotlin.commonizer.mergedtree.CirRootNode
import org.jetbrains.kotlin.commonizer.mergedtree.CirTypeAliasNode
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality

internal object InlineTypeAliasCirNodeTransformer : CirNodeTransformer {
    override fun invoke(root: CirRootNode) {
        root.modules.values.forEach(::invoke)
    }

    private operator fun invoke(module: CirModuleNode) {
        val classNodeIndex = ClassNodeIndex(module)
        module.packages.values.flatMap { pkg -> pkg.typeAliases.values }.forEach { typeAliasNode ->
            inlineTypeAliasIfPossible(classNodeIndex, typeAliasNode)
        }
    }

    private fun inlineTypeAliasIfPossible(classes: ClassNodeIndex, typeAliasNode: CirTypeAliasNode) {
        val targetClassNode = classes[typeAliasNode.id] ?: return

        typeAliasNode.targetDeclarations.forEachIndexed { targetIndex, typeAlias ->
            if (typeAlias != null) {
                inlineTypeAliasIfPossible(classes, typeAlias, targetClassNode, targetIndex)
            }
        }
    }

    private fun inlineTypeAliasIfPossible(
        classes: ClassNodeIndex, typeAlias: CirTypeAlias, targetClassNode: CirClassNode, targetIndex: Int
    ) {
        if (targetClassNode.targetDeclarations[targetIndex] != null) return // No empty spot to inline the type-alias into

        val aliasedClassNode = classes[typeAlias.expandedType.classifierId]

        val artificialAliasedClass = ArtificialAliasedCirClass(
            pointingTypeAlias = typeAlias,
            pointedClass = aliasedClassNode?.targetDeclarations?.get(targetIndex) ?: typeAlias.toArtificialCirClass()
        )

        targetClassNode.targetDeclarations[targetIndex] = artificialAliasedClass

        aliasedClassNode?.constructors?.forEach { (key, aliasedConstructorNode) ->
            val aliasedConstructor = aliasedConstructorNode.targetDeclarations[targetIndex] ?: return@forEach
            targetClassNode.constructors[key]?.targetDeclarations
                ?.set(targetIndex, aliasedConstructor.withContainingClass(artificialAliasedClass).markedArtificial())
        }

        aliasedClassNode?.functions?.forEach { (key, aliasedFunctionNode) ->
            val aliasedFunction = aliasedFunctionNode.targetDeclarations[targetIndex] ?: return@forEach
            targetClassNode.functions[key]?.targetDeclarations
                ?.set(targetIndex, aliasedFunction.withContainingClass(artificialAliasedClass).markedArtificial())
        }

        aliasedClassNode?.properties?.forEach { (key, aliasedPropertyNode) ->
            val aliasedProperty = aliasedPropertyNode.targetDeclarations[targetIndex] ?: return@forEach
            targetClassNode.properties[key]?.targetDeclarations
                ?.set(targetIndex, aliasedProperty.withContainingClass(artificialAliasedClass).markedArtificial())
        }
    }
}

private typealias ClassNodeIndex = Map<CirEntityId, CirClassNode>

private fun ClassNodeIndex(module: CirModuleNode): ClassNodeIndex = module.packages.values
    .flatMap { pkg -> pkg.classes.values }
    .associateBy { clazz -> clazz.id }

private data class ArtificialAliasedCirClass(
    val pointingTypeAlias: CirTypeAlias,
    val pointedClass: CirClass
) : CirClass by pointedClass, ArtificialCirDeclaration {
    override val name: CirName = pointingTypeAlias.name
}

private fun CirTypeAlias.toArtificialCirClass(): CirClass = CirClass.create(
    annotations = emptyList(), name = name, typeParameters = emptyList(),
    visibility = this.visibility, modality = Modality.FINAL, kind = ClassKind.CLASS,
    companion = null, isCompanion = false, isData = false, isValue = false, isInner = false, isExternal = false
).also { it.supertypes = emptyList() }.markedArtificial()
