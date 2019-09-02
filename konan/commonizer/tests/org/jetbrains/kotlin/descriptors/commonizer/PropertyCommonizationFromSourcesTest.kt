/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer

import kotlin.contracts.ExperimentalContracts

@ExperimentalContracts
class PropertyCommonizationFromSourcesTest : AbstractCommonizationFromSourcesTest() {

    fun testMismatchedPackages() = doTestSuccessfulCommonization()

    fun testReturnTypes() = doTestSuccessfulCommonization()

    fun testVisibility() = doTestSuccessfulCommonization()

    fun testSpecificProperties() = doTestSuccessfulCommonization()

    fun testExtensionReceivers() = doTestSuccessfulCommonization()

    fun testSetters() = doTestSuccessfulCommonization()

    fun testAnnotations() = doTestSuccessfulCommonization()

    // TODO: test modality (possible only inside classes)
    // TODO: test virtual val visibility commonization (possible only inside classes)

}
