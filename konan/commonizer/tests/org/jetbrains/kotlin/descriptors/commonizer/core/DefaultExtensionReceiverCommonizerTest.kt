/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.commonizer.EMPTY_CLASSIFIERS_CACHE
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.ExtensionReceiver
import org.jetbrains.kotlin.descriptors.commonizer.mockClassType
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.refinement.TypeRefinement
import org.junit.Test

@TypeRefinement
class DefaultExtensionReceiverCommonizerTest : AbstractCommonizerTest<ExtensionReceiver?, UnwrappedType?>() {

    @Test
    fun nullReceiver() = doTestSuccess(
        null,
        null,
        null,
        null
    )

    @Test
    fun sameReceiver() = doTestSuccess(
        mockClassType("kotlin.String").unwrap(),
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

@TypeRefinement
private fun mockExtensionReceiver(typeFqName: String) = ExtensionReceiver(
    annotations = Annotations.EMPTY,
    type = mockClassType(typeFqName).unwrap()
)
