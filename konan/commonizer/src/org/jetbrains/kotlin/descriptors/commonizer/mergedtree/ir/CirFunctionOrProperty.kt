/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirExtensionReceiver.Companion.toReceiver
import org.jetbrains.kotlin.types.UnwrappedType
import kotlin.LazyThreadSafetyMode.PUBLICATION

interface CirFunctionOrProperty : CirAnnotatedDeclaration, CirNamedDeclaration, CirDeclarationWithTypeParameters, CirDeclarationWithVisibility, CirDeclarationWithModality, CirMaybeCallableMemberOfClass {
    val isExternal: Boolean
    val extensionReceiver: CirExtensionReceiver?
    val returnType: UnwrappedType
    val kind: CallableMemberDescriptor.Kind
}

abstract class CirCommonFunctionOrProperty : CirFunctionOrProperty {
    final override val annotations get() = Annotations.EMPTY
    final override val containingClassKind: ClassKind? get() = unsupported()
    final override val containingClassModality: Modality? get() = unsupported()
    final override val containingClassIsData: Boolean? get() = unsupported()
}

abstract class CirWrappedFunctionOrProperty<T : CallableMemberDescriptor>(protected val wrapped: T) : CirFunctionOrProperty {
    final override val annotations get() = wrapped.annotations
    final override val name get() = wrapped.name
    final override val modality get() = wrapped.modality
    final override val visibility get() = wrapped.visibility
    final override val isExternal get() = wrapped.isExternal
    final override val extensionReceiver by lazy(PUBLICATION) { wrapped.extensionReceiverParameter?.toReceiver() }
    final override val returnType by lazy(PUBLICATION) { wrapped.returnType!!.unwrap() }
    final override val kind get() = wrapped.kind
    final override val containingClassKind: ClassKind? get() = containingClass?.kind
    final override val containingClassModality: Modality? get() = containingClass?.modality
    final override val containingClassIsData: Boolean? get() = containingClass?.isData
    final override val typeParameters by lazy(PUBLICATION) { wrapped.typeParameters.map(::CirWrappedTypeParameter) }
    private val containingClass: ClassDescriptor? get() = wrapped.containingDeclaration as? ClassDescriptor
}

data class CirExtensionReceiver(
    val annotations: Annotations,
    val type: UnwrappedType
) {
    companion object {
        fun UnwrappedType.toReceiverNoAnnotations() = CirExtensionReceiver(Annotations.EMPTY, this)
        fun ReceiverParameterDescriptor.toReceiver() = CirExtensionReceiver(annotations, type.unwrap())
    }
}

fun CirFunctionOrProperty.isNonAbstractMemberInInterface() =
    modality != Modality.ABSTRACT && containingClassKind == ClassKind.INTERFACE

fun CirFunctionOrProperty.isVirtual() =
    visibility != Visibilities.PRIVATE
            && modality != Modality.FINAL
            && !(containingClassModality == Modality.FINAL && containingClassKind != ClassKind.ENUM_CLASS)
