/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.DELEGATION
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.SYNTHESIZED
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirFunctionOrProperty
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirClassifiersCache
import org.jetbrains.kotlin.name.Name

abstract class AbstractFunctionOrPropertyCommonizer<T : CirFunctionOrProperty>(
    cache: CirClassifiersCache
) : AbstractStandardCommonizer<T, T>() {
    protected lateinit var name: Name
    protected val modality = ModalityCommonizer()
    protected val visibility = VisibilityCommonizer.lowering()
    protected val extensionReceiver = ExtensionReceiverCommonizer(cache)
    protected val returnType = TypeCommonizer(cache)
    protected lateinit var kind: CallableMemberDescriptor.Kind
    protected val typeParameters = TypeParameterListCommonizer(cache)

    override fun initialize(first: T) {
        name = first.name
        kind = first.kind
    }

    override fun doCommonizeWith(next: T): Boolean =
        !next.isNonAbstractMemberInInterface() // non-abstract callable members declared in interface can't be commonized
                && next.kind != DELEGATION // delegated members should not be commonized
                && (next.kind != SYNTHESIZED || next.containingClassDetails?.isData != true) // synthesized members of data classes should not be commonized
                && kind == next.kind
                && modality.commonizeWith(next.modality)
                && visibility.commonizeWith(next)
                && extensionReceiver.commonizeWith(next.extensionReceiver)
                && returnType.commonizeWith(next.returnType)
                && typeParameters.commonizeWith(next.typeParameters)
}
