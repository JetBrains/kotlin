/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.mockClassType
import org.jetbrains.kotlin.descriptors.commonizer.mockProperty
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.refinement.TypeRefinement
import org.junit.Test

@TypeRefinement
class DefaultExtensionReceiverCommonizerTest : AbstractCommonizerTest<ReceiverParameterDescriptor?, UnwrappedType?>() {

    @Test
    fun nullReceiver() = doTestSuccess(
        null,
        mock().extensionReceiverParameter,
        mock().extensionReceiverParameter,
        mock().extensionReceiverParameter
    )

    @Test
    fun sameReceiver() = doTestSuccess(
        mockClassType("kotlin.String").unwrap(),
        mock(receiverTypeFqName = "kotlin.String").extensionReceiverParameter,
        mock(receiverTypeFqName = "kotlin.String").extensionReceiverParameter,
        mock(receiverTypeFqName = "kotlin.String").extensionReceiverParameter
    )

    @Test(expected = IllegalStateException::class)
    fun differentReceivers() = doTestFailure(
        mock(receiverTypeFqName = "kotlin.String").extensionReceiverParameter,
        mock(receiverTypeFqName = "kotlin.String").extensionReceiverParameter,
        mock(receiverTypeFqName = "kotlin.Int").extensionReceiverParameter
    )

    @Test(expected = IllegalStateException::class)
    fun nullAndNonNullReceivers1() = doTestFailure(
        mock(receiverTypeFqName = "kotlin.String").extensionReceiverParameter,
        mock(receiverTypeFqName = "kotlin.String").extensionReceiverParameter,
        mock(receiverTypeFqName = null).extensionReceiverParameter
    )

    @Test(expected = IllegalStateException::class)
    fun nullAndNonNullReceivers2() = doTestFailure(
        mock(receiverTypeFqName = null).extensionReceiverParameter,
        mock(receiverTypeFqName = null).extensionReceiverParameter,
        mock(receiverTypeFqName = "kotlin.String").extensionReceiverParameter
    )

    override fun createCommonizer() = ExtensionReceiverCommonizer.default()
}

@TypeRefinement
private fun mock(name: String = "myLength", receiverTypeFqName: String? = null) = mockProperty(
    name = name,
    setterVisibility = null,
    extensionReceiverType = receiverTypeFqName?.let { mockClassType(receiverTypeFqName) },
    returnType = mockClassType("kotlin.Int")
)
