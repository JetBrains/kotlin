/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import kotlin.contracts.ExperimentalContracts

@ExperimentalContracts
class ClassifierCommonizationFromSourcesTest : AbstractCommonizationFromSourcesTest() {

    fun testClassKindAndModifiers() = ignore { doTestSuccessfulCommonization() }

    fun testModality() = doTestSuccessfulCommonization()

    fun testVisibility() = doTestSuccessfulCommonization()

    fun testConstructors() = ignore { doTestSuccessfulCommonization() }

    fun testTypeParameters() = doTestSuccessfulCommonization()

    fun testSupertypes() = doTestSuccessfulCommonization()

    fun testTypeAliases() = doTestSuccessfulCommonization()

    fun testDifferentTypeAliasesInArguments() = doTestSuccessfulCommonization()

    private inline fun ignore(block: () -> Unit) {
        try {
            block()
            error("Test is passing, remove `ignore` call")
        } catch (e: AssertionError) {
            return
        }
    }
}
