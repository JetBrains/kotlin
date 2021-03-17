/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import kotlin.contracts.ExperimentalContracts

@ExperimentalContracts
class ClassifierCommonizationFromSourcesTest : AbstractCommonizationFromSourcesTest() {

    fun testClassKindAndModifiers() = doTestSuccessfulCommonization()

    fun testModality() = doTestSuccessfulCommonization()

    fun testVisibility() = doTestSuccessfulCommonization()

    fun testConstructors() = doTestSuccessfulCommonization()

    fun testTypeParameters() = doTestSuccessfulCommonization()

    fun testSupertypes() = doTestSuccessfulCommonization()

    fun testTypeAliases() = doTestSuccessfulCommonization()

    fun testDifferentTypeAliasesInArguments() = doTestSuccessfulCommonization()
}
