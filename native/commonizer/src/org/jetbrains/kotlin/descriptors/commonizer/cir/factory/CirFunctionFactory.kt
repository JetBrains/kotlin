/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.factory

import kotlinx.metadata.Flag
import kotlinx.metadata.KmFunction
import kotlinx.metadata.klib.annotations
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.commonizer.cir.*
import org.jetbrains.kotlin.descriptors.commonizer.cir.impl.CirFunctionImpl
import org.jetbrains.kotlin.descriptors.commonizer.metadata.decodeCallableKind
import org.jetbrains.kotlin.descriptors.commonizer.metadata.decodeModality
import org.jetbrains.kotlin.descriptors.commonizer.metadata.decodeVisibility
import org.jetbrains.kotlin.descriptors.commonizer.utils.compactMap

object CirFunctionFactory {
    fun create(source: SimpleFunctionDescriptor, containingClass: CirContainingClass?): CirFunction = create(
        annotations = source.annotations.compactMap(CirAnnotationFactory::create),
        name = CirName.create(source.name),
        typeParameters = source.typeParameters.compactMap(CirTypeParameterFactory::create),
        visibility = source.visibility,
        modality = source.modality,
        containingClass = containingClass,
        valueParameters = source.valueParameters.compactMap(CirValueParameterFactory::create),
        hasStableParameterNames = source.hasStableParameterNames(),
        extensionReceiver = source.extensionReceiverParameter?.let(CirExtensionReceiverFactory::create),
        returnType = CirTypeFactory.create(source.returnType!!),
        kind = source.kind,
        modifiers = CirFunctionModifiersFactory.create(source),
    )

    fun create(name: CirName, source: KmFunction, containingClass: CirContainingClass?, typeResolver: CirTypeResolver): CirFunction {
        return create(
            annotations = CirAnnotationFactory.createAnnotations(source.flags, typeResolver, source::annotations),
            name = name,
            typeParameters = source.typeParameters.compactMap { CirTypeParameterFactory.create(it, typeResolver) },
            visibility = decodeVisibility(source.flags),
            modality = decodeModality(source.flags),
            containingClass = containingClass,
            valueParameters = source.valueParameters.compactMap { CirValueParameterFactory.create(it, typeResolver) },
            hasStableParameterNames = !Flag.Function.HAS_NON_STABLE_PARAMETER_NAMES(source.flags),
            extensionReceiver = source.receiverParameterType?.let { CirExtensionReceiverFactory.create(it, typeResolver) },
            returnType = CirTypeFactory.create(source.returnType, typeResolver),
            kind = decodeCallableKind(source.flags),
            modifiers = CirFunctionModifiersFactory.create(source),
        )
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun create(
        annotations: List<CirAnnotation>,
        name: CirName,
        typeParameters: List<CirTypeParameter>,
        visibility: DescriptorVisibility,
        modality: Modality,
        containingClass: CirContainingClass?,
        valueParameters: List<CirValueParameter>,
        hasStableParameterNames: Boolean,
        extensionReceiver: CirExtensionReceiver?,
        returnType: CirType,
        kind: CallableMemberDescriptor.Kind,
        modifiers: CirFunctionModifiers
    ): CirFunction {
        return CirFunctionImpl(
            annotations = annotations,
            name = name,
            typeParameters = typeParameters,
            visibility = visibility,
            modality = modality,
            containingClass = containingClass,
            valueParameters = valueParameters,
            hasStableParameterNames = hasStableParameterNames,
            extensionReceiver = extensionReceiver,
            returnType = returnType,
            kind = kind,
            modifiers = modifiers
        )
    }
}
