/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.factory

import kotlinx.metadata.KmType
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirExtensionReceiver

object CirExtensionReceiverFactory {
    fun create(receiverParameterType: KmType, typeResolver: CirTypeResolver): CirExtensionReceiver = CirExtensionReceiver.create(
        annotations = emptyList(), // TODO nowhere to read receiver annotations from, see KT-42490
        type = CirTypeFactory.create(receiverParameterType, typeResolver)
    )
}
