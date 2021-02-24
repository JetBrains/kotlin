/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer

import kotlin.contracts.ExperimentalContracts

@ExperimentalContracts
class FunctionCommonizationFromSourcesTest : AbstractCommonizationFromSourcesTest() {

    fun testValueParameters() = doTestSuccessfulCommonization()

    fun testAnnotations() = doTestSuccessfulCommonization()

    fun testSpecifics() = doTestSuccessfulCommonization()

    fun testSignaturesWithNullableTypealiases() = doTestSuccessfulCommonization()

    fun testOverloadingByUpperBounds() = doTestSuccessfulCommonization()
}
