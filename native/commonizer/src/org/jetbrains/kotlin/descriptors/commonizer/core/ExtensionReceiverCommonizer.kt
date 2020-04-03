/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirClassifiersCache
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirExtensionReceiver
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirType
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirExtensionReceiver.Companion.toReceiverNoAnnotations

interface ExtensionReceiverCommonizer : Commonizer<CirExtensionReceiver?, CirExtensionReceiver?> {
    companion object {
        fun default(cache: CirClassifiersCache): ExtensionReceiverCommonizer = DefaultExtensionReceiverCommonizer(cache)
    }
}

private class DefaultExtensionReceiverCommonizer(cache: CirClassifiersCache) :
    ExtensionReceiverCommonizer,
    AbstractNullableCommonizer<CirExtensionReceiver, CirExtensionReceiver, CirType, CirType>(
        wrappedCommonizerFactory = { TypeCommonizer.default(cache) },
        extractor = { it.type },
        builder = { it.toReceiverNoAnnotations() }
    )
