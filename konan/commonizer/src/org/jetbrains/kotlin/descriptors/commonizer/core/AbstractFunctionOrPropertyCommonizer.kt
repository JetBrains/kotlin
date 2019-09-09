/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.ClassifiersCache
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.FunctionOrProperty
import org.jetbrains.kotlin.name.Name

abstract class AbstractFunctionOrPropertyCommonizer<T : FunctionOrProperty>(cache: ClassifiersCache) : AbstractStandardCommonizer<T, T>() {
    protected lateinit var name: Name
    protected val modality = ModalityCommonizer.default()
    protected val visibility = VisibilityCommonizer.lowering()
    protected val extensionReceiver = ExtensionReceiverCommonizer.default(cache)
    protected val returnType = TypeCommonizer.default(cache)
    protected val typeParameters = TypeParameterListCommonizer.default(cache)

    override fun initialize(first: T) {
        name = first.name
    }

    override fun doCommonizeWith(next: T) =
        !next.isNonAbstractCallableMemberInInterface // non-abstract callable members declared in interface can't be commonized
                && modality.commonizeWith(next.modality)
                && visibility.commonizeWith(next)
                && extensionReceiver.commonizeWith(next.extensionReceiver)
                && returnType.commonizeWith(next.returnType)
                && typeParameters.commonizeWith(next.typeParameters)
}
