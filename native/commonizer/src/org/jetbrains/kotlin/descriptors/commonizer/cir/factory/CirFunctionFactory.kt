/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.factory

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.commonizer.cir.*
import org.jetbrains.kotlin.descriptors.commonizer.cir.impl.CirFunctionImpl
import org.jetbrains.kotlin.descriptors.commonizer.utils.intern
import org.jetbrains.kotlin.name.Name

object CirFunctionFactory {
    fun create(source: SimpleFunctionDescriptor): CirFunction {
        val containingClass: ClassDescriptor? = source.containingDeclaration as? ClassDescriptor

        return create(
            annotations = source.annotations.map(CirAnnotationFactory::create),
            name = source.name.intern(),
            typeParameters = source.typeParameters.map(CirTypeParameterFactory::create),
            visibility = source.visibility,
            modality = source.modality,
            containingClassDetails = containingClass?.let {
                CirContainingClassDetails(
                    kind = it.kind,
                    modality = it.modality,
                    isData = it.isData
                )
            },
            valueParameters = source.valueParameters.map(CirValueParameterFactory::create),
            hasStableParameterNames = source.hasStableParameterNames(),
            hasSynthesizedParameterNames = source.hasSynthesizedParameterNames(),
            isExternal = source.isExternal,
            extensionReceiver = source.extensionReceiverParameter?.let(CirExtensionReceiverFactory::create),
            returnType = CirTypeFactory.create(source.returnType!!),
            kind = source.kind,
            // TODO: inline?
            modifiers = CirFunctionModifiers(
                isOperator = source.isOperator,
                isInfix = source.isInfix,
                isInline = source.isInline,
                isTailrec = source.isTailrec,
                isSuspend = source.isSuspend,
                isExternal = source.isExternal
            )
        )
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun create(
        annotations: List<CirAnnotation>,
        name: Name,
        typeParameters: List<CirTypeParameter>,
        visibility: Visibility,
        modality: Modality,
        containingClassDetails: CirContainingClassDetails?,
        valueParameters: List<CirValueParameter>,
        hasStableParameterNames: Boolean,
        hasSynthesizedParameterNames: Boolean,
        isExternal: Boolean,
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
            hasSynthesizedParameterNames = hasSynthesizedParameterNames,
            isExternal = isExternal,
            extensionReceiver = extensionReceiver,
            returnType = returnType,
            kind = kind,
            modifiers = modifiers
        )
    }
}
