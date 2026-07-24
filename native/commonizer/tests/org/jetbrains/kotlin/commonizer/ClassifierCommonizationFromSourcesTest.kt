/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import org.junit.jupiter.api.Test

class ClassifierCommonizationFromSourcesTest : AbstractCommonizationFromSourcesTest() {

    @Test
    fun testClassKindAndModifiers() = doTestSuccessfulCommonization()

    @Test
    fun testModality() = doTestSuccessfulCommonization()

    @Test
    fun testVisibility() = doTestSuccessfulCommonization()

    @Test
    fun testConstructors() = doTestSuccessfulCommonization()

    @Test
    fun testTypeParameters() = doTestSuccessfulCommonization()

    @Test
    fun testSupertypes() = doTestSuccessfulCommonization()

    @Test
    fun testTypeAliases() = doTestSuccessfulCommonization()

    @Test
    fun testDifferentTypeAliasesInArguments() = doTestSuccessfulCommonization()
}
