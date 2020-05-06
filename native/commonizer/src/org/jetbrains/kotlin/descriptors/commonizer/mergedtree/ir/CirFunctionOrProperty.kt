/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirExtensionReceiver.Companion.toReceiver
import org.jetbrains.kotlin.descriptors.commonizer.utils.intern

interface CirFunctionOrProperty : CirAnnotatedDeclaration, CirNamedDeclaration, CirDeclarationWithTypeParameters,
    CirDeclarationWithVisibility, CirDeclarationWithModality, CirMaybeCallableMemberOfClass {
    val isExternal: Boolean
    val extensionReceiver: CirExtensionReceiver?
    val returnType: CirType
    val kind: CallableMemberDescriptor.Kind
}

abstract class CirCommonFunctionOrProperty : CirFunctionOrProperty {
    final override val containingClassKind: ClassKind? get() = unsupported()
    final override val containingClassModality: Modality? get() = unsupported()
    final override val containingClassIsData: Boolean? get() = unsupported()
}

abstract class CirFunctionOrPropertyImpl<T : CallableMemberDescriptor>(original: T) : CirFunctionOrProperty {
    final override val annotations = original.annotations.map(CirAnnotation.Companion::create)
    final override val name = original.name.intern()
    final override val modality = original.modality
    final override val visibility = original.visibility
    final override val isExternal = original.isExternal
    final override val extensionReceiver = original.extensionReceiverParameter?.toReceiver()
    final override val returnType = CirType.create(original.returnType!!)
    final override val kind = original.kind
    final override val containingClassKind: ClassKind?
    final override val containingClassModality: Modality?
    final override val containingClassIsData: Boolean?
    final override val typeParameters = original.typeParameters.map(::CirTypeParameterImpl)

    init {
        val containingClass = original.containingDeclaration as? ClassDescriptor
        containingClassKind = containingClass?.kind
        containingClassModality = containingClass?.modality
        containingClassIsData = containingClass?.isData
    }
}

data class CirExtensionReceiver(
    val annotations: List<CirAnnotation>,
    val type: CirType
) {
    companion object {
        fun CirType.toReceiverNoAnnotations() = CirExtensionReceiver(annotations = emptyList(), type = this)

        fun ReceiverParameterDescriptor.toReceiver() = CirExtensionReceiver(
            annotations = annotations.map(CirAnnotation.Companion::create),
            type = CirType.create(type)
        )
    }
}

fun CirFunctionOrProperty.isNonAbstractMemberInInterface() =
    modality != Modality.ABSTRACT && containingClassKind == ClassKind.INTERFACE

fun CirFunctionOrProperty.isVirtual() =
    visibility != Visibilities.PRIVATE
            && modality != Modality.FINAL
            && !(containingClassModality == Modality.FINAL && containingClassKind != ClassKind.ENUM_CLASS)
