/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.cir.CirExtensionReceiver
import org.jetbrains.kotlin.commonizer.utils.MOCK_CLASSIFIERS
import org.jetbrains.kotlin.commonizer.utils.mockClassType
import org.junit.Test

class ExtensionReceiverCommonizerTest : AbstractCommonizerTest<CirExtensionReceiver?, CirExtensionReceiver?>() {

    @Test
    fun nullReceiver() = doTestSuccess(
        expected = null,
        null, null, null
    )

    @Test
    fun sameReceiver() = doTestSuccess(
        expected = mockExtensionReceiver("kotlin/String"),
        mockExtensionReceiver("kotlin/String"),
        mockExtensionReceiver("kotlin/String"),
        mockExtensionReceiver("kotlin/String")
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun differentReceivers() = doTestFailure(
        mockExtensionReceiver("kotlin/String"),
        mockExtensionReceiver("kotlin/String"),
        mockExtensionReceiver("kotlin/Int")
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun nullAndNonNullReceivers1() = doTestFailure(
        mockExtensionReceiver("kotlin/String"),
        mockExtensionReceiver("kotlin/String"),
        null
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun nullAndNonNullReceivers2() = doTestFailure(
        null,
        null,
        mockExtensionReceiver("kotlin/String")
    )

    override fun createCommonizer() = ExtensionReceiverCommonizer(MOCK_CLASSIFIERS)
}

private fun mockExtensionReceiver(receiverClassId: String) = CirExtensionReceiver.create(
    annotations = emptyList(),
    type = mockClassType(receiverClassId)
)
