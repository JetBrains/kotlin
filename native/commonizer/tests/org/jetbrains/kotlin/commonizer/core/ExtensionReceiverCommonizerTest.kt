/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.AbstractInlineSourcesCommonizationTest
import org.jetbrains.kotlin.commonizer.DefaultCommonizerSettings
import org.jetbrains.kotlin.commonizer.core.ExtensionReceiverCommonizer.Commonized
import org.jetbrains.kotlin.commonizer.utils.MOCK_CLASSIFIERS
import org.jetbrains.kotlin.commonizer.utils.mockExtensionReceiver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExtensionReceiverCommonizerTest : AbstractInlineSourcesCommonizationTest() {

    private val commonizer = ExtensionReceiverCommonizer(TypeCommonizer(MOCK_CLASSIFIERS, DefaultCommonizerSettings))

    @Test
    fun `test null receiver`() {
        assertEquals(
            Commonized(null), commonizer(listOf(null, null, null)),
        )
    }

    @Test
    fun `test same receiver`() {
        assertEquals(
            Commonized(mockExtensionReceiver("kotlin/String")),
            commonizer(
                listOf(
                    mockExtensionReceiver("kotlin/String"),
                    mockExtensionReceiver("kotlin/String"),
                    mockExtensionReceiver("kotlin/String")
                )
            )
        )
    }

    @Test
    fun `test different receiver`() {
        assertEquals(
            null, commonizer(
                listOf(
                    mockExtensionReceiver("kotlin/String"),
                    mockExtensionReceiver("kotlin/String"),
                    mockExtensionReceiver("kotlin/Int")
                )
            )
        )
    }

    @Test
    fun `test null and non-null receivers - 1`() {
        assertEquals(
            null, commonizer(
                listOf(
                    mockExtensionReceiver("kotlin/String"),
                    mockExtensionReceiver("kotlin/String"),
                    null
                )
            )
        )
    }

    @Test
    fun `test null and non-null receivers - 2`() {
        assertEquals(
            null, commonizer(listOf(null, null, mockExtensionReceiver("kotlin/String")))
        )
    }
}
