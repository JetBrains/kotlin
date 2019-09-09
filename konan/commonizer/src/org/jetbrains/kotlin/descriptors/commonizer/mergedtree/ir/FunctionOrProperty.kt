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

interface FunctionOrProperty : AnnotatedDeclaration, NamedDeclaration, DeclarationWithTypeParameters, MaybeVirtualCallableMember {
    val modality: Modality
    val isExternal: Boolean
    val extensionReceiver: ExtensionReceiver?
    val returnType: UnwrappedType
    val kind: CallableMemberDescriptor.Kind
    val isNonAbstractCallableMemberInInterface: Boolean
}

abstract class CommonFunctionOrProperty : FunctionOrProperty {
    final override val annotations get() = Annotations.EMPTY
    final override val kind get() = CallableMemberDescriptor.Kind.DECLARATION
    final override val isVirtual get() = unsupported()
    final override val isNonAbstractCallableMemberInInterface get() = unsupported()
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
    final override val isVirtual get() = descriptor.isOverridable
    final override val isNonAbstractCallableMemberInInterface
        get() = modality != Modality.ABSTRACT && (descriptor.containingDeclaration as? ClassDescriptor)?.kind == ClassKind.INTERFACE
    final override val typeParameters by lazy(PUBLICATION) { descriptor.typeParameters.map(::TargetTypeParameter) }
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
