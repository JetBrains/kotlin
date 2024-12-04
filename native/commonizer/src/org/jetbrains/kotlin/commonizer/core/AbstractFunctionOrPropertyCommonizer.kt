/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.CommonizerSettings
import org.jetbrains.kotlin.commonizer.cir.*
import org.jetbrains.kotlin.commonizer.mergedtree.CirKnownClassifiers
import org.jetbrains.kotlin.commonizer.utils.singleDistinctValueOrNull
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.DELEGATION
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.SYNTHESIZED
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility

class FunctionOrPropertyBaseCommonizer(
    private val classifiers: CirKnownClassifiers,
    private val settings: CommonizerSettings,
    private val typeCommonizer: TypeCommonizer,
    private val extensionReceiverCommonizer: ExtensionReceiverCommonizer = ExtensionReceiverCommonizer(typeCommonizer),
    private val returnTypeCommonizer: ReturnTypeCommonizer = ReturnTypeCommonizer(typeCommonizer),
) : NullableContextualSingleInvocationCommonizer<CirFunctionOrProperty, FunctionOrPropertyBaseCommonizer.FunctionOrProperty> {

    data class FunctionOrProperty(
        val name: CirName,
        val kind: CallableMemberDescriptor.Kind,
        val modality: Modality,
        val visibility: Visibility,
        val extensionReceiver: CirExtensionReceiver?,
        val returnType: CirType,
        val typeParameters: List<CirTypeParameter>,
        val annotations: List<CirAnnotation>,
    )

    override fun invoke(values: List<CirFunctionOrProperty>): FunctionOrProperty? {
        /* Preconditions */
        if (values.isEmpty()) return null

        // delegated members should not be commonized
        if (values.any { value -> value.kind == DELEGATION }) {
            return null
        }

        // synthesized members of data classes should not be commonized
        if (values.any { value -> value.kind == SYNTHESIZED && value.containingClass?.isData == true }) {
            return null
        }

        val returnType = returnTypeCommonizer(values) ?: return null

        val unsafeNumberAnnotation = createUnsafeNumberAnnotationIfNecessary(
            classifiers.classifierIndices.targets, settings,
            inputDeclarations = values,
            inputTypes = values.map { it.returnType },
            commonizedType = returnType,
        )

        val annotations = AnnotationsCommonizer.commonize(values.map { it.annotations }).orEmpty()
            .plus(listOfNotNull(unsafeNumberAnnotation))

        return FunctionOrProperty(
            name = values.first().name,
            kind = values.singleDistinctValueOrNull { it.kind } ?: return null,
            modality = ModalityCommonizer().commonize(values.map { it.modality }) ?: return null,
            visibility = VisibilityCommonizer.lowering().commonize(values) ?: return null,
            extensionReceiver = (extensionReceiverCommonizer(values.map { it.extensionReceiver }) ?: return null).receiver,
            returnType = returnTypeCommonizer(values) ?: return null,
            typeParameters = TypeParameterListCommonizer(typeCommonizer).commonize(values.map { it.typeParameters }) ?: return null,
            annotations = annotations
        )
    }
}
