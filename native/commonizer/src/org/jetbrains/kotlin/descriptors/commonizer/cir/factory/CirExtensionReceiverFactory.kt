/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.factory

import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirAnnotation
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirExtensionReceiver
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirType
import org.jetbrains.kotlin.descriptors.commonizer.cir.impl.CirExtensionReceiverImpl

object CirExtensionReceiverFactory {
    fun create(source: ReceiverParameterDescriptor): CirExtensionReceiver = create(
        annotations = source.annotations.map(CirAnnotationFactory::create),
        type = CirTypeFactory.create(source.type)
    )

    @Suppress("NOTHING_TO_INLINE")
    inline fun create(
        annotations: List<CirAnnotation>,
        type: CirType
    ): CirExtensionReceiver {
        return CirExtensionReceiverImpl(
            annotations = annotations,
            type = type
        )
    }
}
