/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.ClassifiersCache
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.ExtensionReceiver
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.UnwrappedType

interface ExtensionReceiverCommonizer : Commonizer<ExtensionReceiver?, UnwrappedType?> {
    companion object {
        fun default(cache: ClassifiersCache): ExtensionReceiverCommonizer = DefaultExtensionReceiverCommonizer(cache)
    }
}

private class DefaultExtensionReceiverCommonizer(cache: ClassifiersCache) :
    ExtensionReceiverCommonizer,
    AbstractNullableCommonizer<ExtensionReceiver, UnwrappedType, KotlinType, UnwrappedType>(
        wrappedCommonizerFactory = { TypeCommonizer.default(cache) },
        extractor = { it.type },
        builder = { it }
    )
