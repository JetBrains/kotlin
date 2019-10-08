/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirExtensionReceiver.Companion.toReceiver
import kotlin.LazyThreadSafetyMode.PUBLICATION

interface CirFunctionOrProperty : CirAnnotatedDeclaration, CirNamedDeclaration, CirDeclarationWithTypeParameters, CirDeclarationWithVisibility, CirDeclarationWithModality, CirMaybeCallableMemberOfClass {
    val isExternal: Boolean
    val extensionReceiver: CirExtensionReceiver?
    val returnType: CirType
    val kind: CallableMemberDescriptor.Kind
}

abstract class CirCommonFunctionOrProperty : CirFunctionOrProperty {
    final override val annotations: List<CirAnnotation> get() = emptyList() // TODO: commonize annotations, KT-34234
    final override val containingClassKind: ClassKind? get() = unsupported()
    final override val containingClassModality: Modality? get() = unsupported()
    final override val containingClassIsData: Boolean? get() = unsupported()
}

abstract class CirWrappedFunctionOrProperty<T : CallableMemberDescriptor>(protected val wrapped: T) : CirFunctionOrProperty {
    final override val annotations by lazy(PUBLICATION) { wrapped.annotations.map(::CirAnnotation) }
    final override val name get() = wrapped.name
    final override val modality get() = wrapped.modality
    final override val visibility get() = wrapped.visibility
    final override val isExternal get() = wrapped.isExternal
    final override val extensionReceiver by lazy(PUBLICATION) { wrapped.extensionReceiverParameter?.toReceiver() }
    final override val returnType by lazy(PUBLICATION) { CirType.create(wrapped.returnType!!) }
    final override val kind get() = wrapped.kind
    final override val containingClassKind: ClassKind? get() = containingClass?.kind
    final override val containingClassModality: Modality? get() = containingClass?.modality
    final override val containingClassIsData: Boolean? get() = containingClass?.isData
    final override val typeParameters by lazy(PUBLICATION) { wrapped.typeParameters.map(::CirWrappedTypeParameter) }
    private val containingClass: ClassDescriptor? get() = wrapped.containingDeclaration as? ClassDescriptor
}

data class CirExtensionReceiver(
    val annotations: List<CirAnnotation>,
    val type: CirType
) {
    companion object {
        fun CirType.toReceiverNoAnnotations() = CirExtensionReceiver( /* TODO: commonize annotations, KT-34234 */ emptyList(), this)
        fun ReceiverParameterDescriptor.toReceiver() = CirExtensionReceiver(annotations.map(::CirAnnotation), CirType.create(type))
    }
}

fun CirFunctionOrProperty.isNonAbstractMemberInInterface() =
    modality != Modality.ABSTRACT && containingClassKind == ClassKind.INTERFACE

fun CirFunctionOrProperty.isVirtual() =
    visibility != Visibilities.PRIVATE
            && modality != Modality.FINAL
            && !(containingClassModality == Modality.FINAL && containingClassKind != ClassKind.ENUM_CLASS)
