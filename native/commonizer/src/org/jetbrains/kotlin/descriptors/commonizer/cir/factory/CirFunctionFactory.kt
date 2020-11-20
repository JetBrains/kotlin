/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.factory

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.commonizer.cir.*
import org.jetbrains.kotlin.descriptors.commonizer.cir.impl.CirFunctionImpl
import org.jetbrains.kotlin.descriptors.commonizer.utils.compactMap
import org.jetbrains.kotlin.descriptors.commonizer.utils.intern
import org.jetbrains.kotlin.name.Name

object CirFunctionFactory {
    fun create(source: SimpleFunctionDescriptor): CirFunction = create(
        annotations = source.annotations.compactMap(CirAnnotationFactory::create),
        name = source.name.intern(),
        typeParameters = source.typeParameters.compactMap(CirTypeParameterFactory::create),
        visibility = source.visibility,
        modality = source.modality,
        containingClassDetails = CirContainingClassDetailsFactory.create(source),
        valueParameters = source.valueParameters.compactMap(CirValueParameterFactory::create),
        hasStableParameterNames = source.hasStableParameterNames(),
        extensionReceiver = source.extensionReceiverParameter?.let(CirExtensionReceiverFactory::create),
        returnType = CirTypeFactory.create(source.returnType!!),
        kind = source.kind,
        modifiers = CirFunctionModifiersFactory.create(source),
    )

    @Suppress("NOTHING_TO_INLINE")
    inline fun create(
        annotations: List<CirAnnotation>,
        name: Name,
        typeParameters: List<CirTypeParameter>,
        visibility: DescriptorVisibility,
        modality: Modality,
        containingClassDetails: CirContainingClassDetails?,
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
            containingClassDetails = containingClassDetails,
            valueParameters = valueParameters,
            hasStableParameterNames = hasStableParameterNames,
            extensionReceiver = extensionReceiver,
            returnType = returnType,
            kind = kind,
            modifiers = modifiers
        )
    }
}
