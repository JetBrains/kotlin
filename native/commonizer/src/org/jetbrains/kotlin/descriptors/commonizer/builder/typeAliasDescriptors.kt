/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.builder

import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirClass
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirClassifier
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirTypeAlias
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirTypeAliasNode
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirNode.Companion.indexOfCommon
import org.jetbrains.kotlin.descriptors.commonizer.utils.CommonizedGroup
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.TypeAliasExpander
import org.jetbrains.kotlin.types.TypeAliasExpansion

internal fun CirTypeAliasNode.buildDescriptors(
    components: GlobalDeclarationsBuilderComponents,
    output: CommonizedGroup<ClassifierDescriptorWithTypeParameters>,
    containingDeclarations: List<DeclarationDescriptor?>
) {
    val commonClassifier: CirClassifier? = commonDeclaration()
    // Note: 'expect class' and lifted up 'typealias' both can't be non-null
    val commonTypeAlias: CirTypeAlias? = commonClassifier as? CirTypeAlias?

    val isLiftedUp = commonTypeAlias?.isLiftedUp == true
    val markAsActual = commonClassifier != null

    if (!isLiftedUp) {
        targetDeclarations.forEachIndexed { index, typeAlias ->
            typeAlias?.buildDescriptor(components, output, index, containingDeclarations, classId, isActual = markAsActual)
        }
    }

    if (commonTypeAlias != null) {
        commonTypeAlias.buildDescriptor(components, output, indexOfCommon, containingDeclarations, classId)
    } else if (commonClassifier != null && commonClassifier is CirClass) {
        commonClassifier.buildDescriptor(components, output, indexOfCommon, containingDeclarations, classId, isExpect = true)
    }
}

private fun CirTypeAlias.buildDescriptor(
    components: GlobalDeclarationsBuilderComponents,
    output: CommonizedGroup<ClassifierDescriptorWithTypeParameters>,
    index: Int,
    containingDeclarations: List<DeclarationDescriptor?>,
    classId: ClassId,
    isActual: Boolean = false
) {
    val targetComponents = components.targetComponents[index]
    val storageManager = targetComponents.storageManager
    val containingDeclaration = containingDeclarations[index] ?: error("No containing declaration for type alias $this")

    val typeAliasDescriptor = CommonizedTypeAliasDescriptor(
        storageManager = storageManager,
        containingDeclaration = containingDeclaration,
        annotations = annotations.buildDescriptors(targetComponents),
        name = name,
        visibility = visibility,
        isActual = isActual
    )

    val (declaredTypeParameters, typeParameterResolver) = typeParameters.buildDescriptorsAndTypeParameterResolver(
        targetComponents,
        TypeParameterResolver.EMPTY,
        typeAliasDescriptor
    )

    val lazyUnderlyingType = storageManager.createLazyValue {
        underlyingType.buildType(targetComponents, typeParameterResolver, expandTypeAliases = false)
    }

    val lazyExpandedType = storageManager.createLazyValue {
        TypeAliasExpander.NON_REPORTING.expandWithoutAbbreviation(
            TypeAliasExpansion.createWithFormalArguments(typeAliasDescriptor),
            Annotations.EMPTY
        )
    }

    typeAliasDescriptor.initialize(
        declaredTypeParameters = declaredTypeParameters,
        underlyingType = lazyUnderlyingType,
        expandedType = lazyExpandedType
    )

    // cache created type alias descriptor:
    components.cache.cache(classId, index, typeAliasDescriptor)

    output[index] = typeAliasDescriptor
}
