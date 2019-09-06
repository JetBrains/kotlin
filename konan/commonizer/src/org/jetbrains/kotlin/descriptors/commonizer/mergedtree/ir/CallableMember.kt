/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.ExtensionReceiver.Companion.toReceiver
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.UnwrappedType

interface CallableMember {
    val annotations: Annotations
    val name: Name
    val modality: Modality
    val visibility: Visibility
    val isExternal: Boolean
    val extensionReceiver: ExtensionReceiver?
    val returnType: UnwrappedType
    val kind: CallableMemberDescriptor.Kind
}

abstract class CommonCallableMember : CallableMember {
    final override val annotations: Annotations get() = Annotations.EMPTY
    final override val kind get() = CallableMemberDescriptor.Kind.DECLARATION
}

abstract class TargetCallableMember<T : CallableMemberDescriptor>(protected val descriptor: T) : CallableMember {
    final override val annotations: Annotations get() = descriptor.annotations
    final override val name: Name get() = descriptor.name
    final override val modality: Modality get() = descriptor.modality
    final override val visibility: Visibility get() = descriptor.visibility
    final override val isExternal: Boolean get() = descriptor.isExternal
    final override val extensionReceiver: ExtensionReceiver? get() = descriptor.extensionReceiverParameter?.toReceiver()
    final override val returnType: UnwrappedType get() = descriptor.returnType!!.unwrap()
    final override val kind: CallableMemberDescriptor.Kind get() = descriptor.kind
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
