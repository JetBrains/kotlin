/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.commonizer.cir.CirExtensionReceiver
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirType
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.CirExtensionReceiverFactory
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirKnownClassifiers

class ExtensionReceiverCommonizer(classifiers: CirKnownClassifiers) :
    AbstractNullableCommonizer<CirExtensionReceiver, CirExtensionReceiver, CirType, CirType>(
        wrappedCommonizerFactory = { TypeCommonizer(classifiers) },
        extractor = { it.type },
        builder = { receiverType ->
            CirExtensionReceiverFactory.create(
                annotations = emptyList(),
                type = receiverType
            )
        }
    )
