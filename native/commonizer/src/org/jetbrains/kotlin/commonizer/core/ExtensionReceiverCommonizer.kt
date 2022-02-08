/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.cir.CirExtensionReceiver

class ExtensionReceiverCommonizer(
    private val typeCommonizer: TypeCommonizer
) : NullableContextualSingleInvocationCommonizer<CirExtensionReceiver?, ExtensionReceiverCommonizer.Commonized> {

    data class Commonized(val receiver: CirExtensionReceiver?) {
        companion object {
            val NULL = Commonized(null)
        }
    }

    override fun invoke(values: List<CirExtensionReceiver?>): Commonized? {
        if (values.all { it == null }) return Commonized.NULL
        if (values.any { it == null }) return null

        return Commonized(
            CirExtensionReceiver(
                annotations = emptyList(),
                type = typeCommonizer(values.map { checkNotNull(it).type }) ?: return null
            )
        )
    }
}
