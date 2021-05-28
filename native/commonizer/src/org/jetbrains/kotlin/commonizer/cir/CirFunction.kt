/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cir

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility

interface CirFunction : CirFunctionOrProperty, CirCallableMemberWithParameters {
    val modifiers: CirFunctionModifiers

    override fun withContainingClass(containingClass: CirContainingClass): CirFunction

    companion object {
        @Suppress("NOTHING_TO_INLINE")
        inline fun create(
            annotations: List<CirAnnotation>,
            name: CirName,
            typeParameters: List<CirTypeParameter>,
            visibility: Visibility,
            modality: Modality,
            containingClass: CirContainingClass?,
            valueParameters: List<CirValueParameter>,
            hasStableParameterNames: Boolean,
            extensionReceiver: CirExtensionReceiver?,
            returnType: CirType,
            kind: CallableMemberDescriptor.Kind,
            modifiers: CirFunctionModifiers
        ): CirFunction = CirFunctionImpl(
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

data class CirFunctionImpl(
    override val annotations: List<CirAnnotation>,
    override val name: CirName,
    override val typeParameters: List<CirTypeParameter>,
    override val visibility: Visibility,
    override val modality: Modality,
    override val containingClass: CirContainingClass?,
    override var valueParameters: List<CirValueParameter>,
    override var hasStableParameterNames: Boolean,
    override val extensionReceiver: CirExtensionReceiver?,
    override val returnType: CirType,
    override val kind: CallableMemberDescriptor.Kind,
    override val modifiers: CirFunctionModifiers
) : CirFunction {
    override fun withContainingClass(containingClass: CirContainingClass): CirFunction {
        return copy(containingClass = containingClass)
    }
}
