/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.transformer

import org.jetbrains.kotlin.commonizer.CommonizerSettings
import org.jetbrains.kotlin.commonizer.cir.*
import org.jetbrains.kotlin.commonizer.mergedtree.*
import org.jetbrains.kotlin.commonizer.mergedtree.CirNodeRelationship.Composite.Companion.plus
import org.jetbrains.kotlin.commonizer.mergedtree.CirNodeRelationship.ParentNode
import org.jetbrains.kotlin.commonizer.mergedtree.CirNodeRelationship.PreferredNode
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.storage.StorageManager

internal class InlineTypeAliasCirNodeTransformer(
    private val storageManager: StorageManager,
    private val classifiers: CirKnownClassifiers,
    private val settings: CommonizerSettings,
) : CirNodeTransformer {
    override fun invoke(root: CirRootNode) {
        root.modules.values.forEach(::invoke)
    }

    private operator fun invoke(module: CirModuleNode) {
        val classNodeIndex = ClassNodeIndex(module)

        module.packages.values.forEach { packageNode ->
            packageNode.typeAliases.values.forEach { typeAliasNode ->
                val targetClassNode = classNodeIndex[typeAliasNode.id] ?: packageNode.createArtificialClassNode(typeAliasNode)
                inlineTypeAliasIfPossible(classNodeIndex, typeAliasNode, targetClassNode)
            }
        }
    }

    private fun inlineTypeAliasIfPossible(classes: ClassNodeIndex, fromTypeAliasNode: CirTypeAliasNode, intoClassNode: CirClassNode) {
        fromTypeAliasNode.targetDeclarations.forEachIndexed { targetIndex, typeAlias ->
            if (typeAlias != null) {
                inlineTypeAliasIfPossible(classes, typeAlias, intoClassNode, targetIndex)
            }
        }
    }

    private fun inlineTypeAliasIfPossible(
        classes: ClassNodeIndex, fromTypeAlias: CirTypeAlias, intoClassNode: CirClassNode, targetIndex: Int
    ) {
        if (fromTypeAlias.typeParameters.isNotEmpty()) {
            // Inlining parameterized TAs is not supported yet
            return
        }

        if (fromTypeAlias.underlyingType.arguments.isNotEmpty() ||
            fromTypeAlias.underlyingType.run { this as? CirClassType }?.outerType?.arguments?.isNotEmpty() == true
        ) {
            // Inlining TAs with parameterized underlying types is not supported yet
            return
        }

        if (intoClassNode.targetDeclarations[targetIndex] != null) {
            // No empty spot to inline the type-alias into
            return
        }

        val fromAliasedClassNode = classes[fromTypeAlias.expandedType.classifierId]

        val intoArtificialClass = ArtificialAliasedCirClass(
            pointingTypeAlias = fromTypeAlias,
            pointedClass = fromAliasedClassNode?.targetDeclarations?.get(targetIndex) ?: fromTypeAlias.toArtificialCirClass(targetIndex)
        )

        intoClassNode.targetDeclarations[targetIndex] = intoArtificialClass

        if (fromAliasedClassNode != null && !fromTypeAlias.expandedType.isMarkedNullable) {
            inlineArtificialMembers(fromAliasedClassNode, intoClassNode, intoArtificialClass, targetIndex)
        }
    }

    private fun inlineArtificialMembers(
        fromAliasedClassNode: CirClassNode,
        intoClassNode: CirClassNode,
        intoClass: CirClass,
        targetIndex: Int
    ) {
        val targetSize = intoClassNode.targetDeclarations.size

        fromAliasedClassNode.constructors.forEach { (key, aliasedConstructorNode) ->
            val aliasedConstructor = aliasedConstructorNode.targetDeclarations[targetIndex] ?: return@forEach
            intoClassNode.constructors.getOrPut(key) {
                buildClassConstructorNode(storageManager, targetSize, classifiers, settings, ParentNode(intoClassNode))
            }.targetDeclarations[targetIndex] = aliasedConstructor.withContainingClass(intoClass)
        }

        fromAliasedClassNode.functions.forEach { (key, aliasedFunctionNode) ->
            val aliasedFunction = aliasedFunctionNode.targetDeclarations[targetIndex] ?: return@forEach
            intoClassNode.functions.getOrPut(key) {
                buildFunctionNode(storageManager, targetSize, classifiers, settings, ParentNode(intoClassNode))
            }.targetDeclarations[targetIndex] = aliasedFunction.withContainingClass(intoClass)
        }

        fromAliasedClassNode.properties.forEach { (key, aliasedPropertyNode) ->
            val aliasedProperty = aliasedPropertyNode.targetDeclarations[targetIndex] ?: return@forEach
            intoClassNode.properties.getOrPut(key) {
                buildPropertyNode(storageManager, targetSize, classifiers, settings, ParentNode(intoClassNode))
            }.targetDeclarations[targetIndex] = aliasedProperty.withContainingClass(intoClass)
        }
    }

    private fun CirPackageNode.createArtificialClassNode(typeAliasNode: CirTypeAliasNode): CirClassNode {
        val classNode = buildClassNode(
            storageManager = storageManager,
            size = typeAliasNode.targetDeclarations.size,
            classifiers = classifiers,
            // This artificial class node should only try to commonize if the package node is commonized
            //  and if the original typeAliasNode cannot be commonized.
            //  Therefore, this artificial class node acts as a fallback with the original type-alias being still the preferred
            //  option for commonization
            nodeRelationship = ParentNode(this) + PreferredNode(typeAliasNode),
            classId = typeAliasNode.id,
            settings = settings,
        )
        this.classes[typeAliasNode.classifierName] = classNode
        return classNode
    }

    private fun CirTypeAlias.toArtificialCirClass(targetIndex: Int): CirClass = CirClass.create(
        annotations = emptyList(), name = name, typeParameters = typeParameters, supertypes = resolveSupertypes(targetIndex),
        visibility = this.visibility, modality = Modality.FINAL, kind = ClassKind.CLASS,
        companion = null, isCompanion = false, isData = false, isValue = false, isInner = false, hasEnumEntries = false
    )

    private fun CirTypeAlias.resolveSupertypes(targetIndex: Int): List<CirType> {
        if (expandedType.isMarkedNullable) return emptyList()
        val resolver = SimpleCirSupertypesResolver(
            classifiers = classifiers.classifierIndices[targetIndex],
            dependencies = CirProvidedClassifiers.of(
                classifiers.commonDependencies, classifiers.targetDependencies[targetIndex]
            )
        )
        return resolver.supertypes(expandedType).toList()
    }
}

private typealias ClassNodeIndex = Map<CirEntityId, CirClassNode>

private fun ClassNodeIndex(module: CirModuleNode): ClassNodeIndex = module.packages.values
    .flatMap { pkg -> pkg.classes.values }
    .associateBy { clazz -> clazz.id }

private data class ArtificialAliasedCirClass(
    val pointingTypeAlias: CirTypeAlias,
    val pointedClass: CirClass
) : CirClass by pointedClass {
    override val name: CirName = pointingTypeAlias.name
    override var companion: CirName?
        get() = null
        set(_) = throw UnsupportedOperationException("Can't set companion on artificial class (pointed by $pointingTypeAlias)")
}



