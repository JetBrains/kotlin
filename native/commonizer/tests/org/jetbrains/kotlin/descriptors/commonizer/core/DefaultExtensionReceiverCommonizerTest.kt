/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.commonizer.utils.EMPTY_CLASSIFIERS_CACHE
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirExtensionReceiver
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirType
import org.jetbrains.kotlin.descriptors.commonizer.utils.mockClassType
import org.junit.Test

class DefaultExtensionReceiverCommonizerTest : AbstractCommonizerTest<CirExtensionReceiver?, CirExtensionReceiver?>() {

    @Test
    fun nullReceiver() = doTestSuccess(
        null,
        null,
        null,
        null
    )

    @Test
    fun sameReceiver() = doTestSuccess(
        mockExtensionReceiver("kotlin.String"),
        mockExtensionReceiver("kotlin.String"),
        mockExtensionReceiver("kotlin.String"),
        mockExtensionReceiver("kotlin.String")
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun differentReceivers() = doTestFailure(
        mockExtensionReceiver("kotlin.String"),
        mockExtensionReceiver("kotlin.String"),
        mockExtensionReceiver("kotlin.Int")
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun nullAndNonNullReceivers1() = doTestFailure(
        mockExtensionReceiver("kotlin.String"),
        mockExtensionReceiver("kotlin.String"),
        null
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun nullAndNonNullReceivers2() = doTestFailure(
        null,
        null,
        mockExtensionReceiver("kotlin.String")
    )

    override fun createCommonizer() = ExtensionReceiverCommonizer.default(EMPTY_CLASSIFIERS_CACHE)
}

private fun mockExtensionReceiver(typeFqName: String) = CirExtensionReceiver(
    annotations = emptyList(),
    type = CirType.create(mockClassType(typeFqName))
)
