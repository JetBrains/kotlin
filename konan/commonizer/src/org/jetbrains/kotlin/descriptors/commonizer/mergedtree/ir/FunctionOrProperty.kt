/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.ExtensionReceiver.Companion.toReceiver
import org.jetbrains.kotlin.types.UnwrappedType
import kotlin.LazyThreadSafetyMode.PUBLICATION

interface FunctionOrProperty : AnnotatedDeclaration, NamedDeclaration, DeclarationWithTypeParameters, DeclarationWithVisibility, DeclarationWithModality, MaybeCallableMemberOfClass {
    val isExternal: Boolean
    val extensionReceiver: ExtensionReceiver?
    val returnType: UnwrappedType
    val kind: CallableMemberDescriptor.Kind
}

abstract class CommonFunctionOrProperty : FunctionOrProperty {
    final override val annotations get() = Annotations.EMPTY
    final override val containingClassKind: ClassKind? get() = unsupported()
    final override val containingClassModality: Modality? get() = unsupported()
    final override val containingClassIsData: Boolean? get() = unsupported()
}

abstract class TargetFunctionOrProperty<T : CallableMemberDescriptor>(protected val descriptor: T) : FunctionOrProperty {
    final override val annotations get() = descriptor.annotations
    final override val name get() = descriptor.name
    final override val modality get() = descriptor.modality
    final override val visibility get() = descriptor.visibility
    final override val isExternal get() = descriptor.isExternal
    final override val extensionReceiver by lazy(PUBLICATION) { descriptor.extensionReceiverParameter?.toReceiver() }
    final override val returnType by lazy(PUBLICATION) { descriptor.returnType!!.unwrap() }
    final override val kind get() = descriptor.kind
    final override val containingClassKind: ClassKind? get() = containingClass?.kind
    final override val containingClassModality: Modality? get() = containingClass?.modality
    final override val containingClassIsData: Boolean? get() = containingClass?.isData
    final override val typeParameters by lazy(PUBLICATION) { descriptor.typeParameters.map(::TargetTypeParameter) }
    private val containingClass: ClassDescriptor? get() = descriptor.containingDeclaration as? ClassDescriptor
}

data class ExtensionReceiver(
    val annotations: Annotations,
    val type: UnwrappedType
) {
    companion object {
        fun UnwrappedType.toReceiverNoAnnotations() = ExtensionReceiver(Annotations.EMPTY, this)
        fun ReceiverParameterDescriptor.toReceiver() = ExtensionReceiver(annotations, type.unwrap())
    }
}

fun FunctionOrProperty.isNonAbstractMemberInInterface() =
    modality != Modality.ABSTRACT && containingClassKind == ClassKind.INTERFACE

fun FunctionOrProperty.isVirtual() =
    visibility != Visibilities.PRIVATE
            && modality != Modality.FINAL
            && !(containingClassModality == Modality.FINAL && containingClassKind != ClassKind.ENUM_CLASS)
