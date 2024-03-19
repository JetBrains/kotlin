/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.klib.reader.tests

import org.jetbrains.kotlin.native.analysis.api.readKlibDeclarationAddresses
import org.jetbrains.kotlin.analysis.api.klib.reader.testUtils.providedTestProjectKlib
import org.jetbrains.kotlin.analysis.api.klib.reader.testUtils.render
import org.jetbrains.kotlin.analysis.api.klib.reader.testUtils.testDataDir
import org.jetbrains.kotlin.test.KotlinTestUtils
import kotlin.test.Test
import kotlin.test.fail

class ReadKlibDeclarationAddressesBlackBoxTest {

    @Test
    fun `test - testProject`() {
        val addresses = readKlibDeclarationAddresses(providedTestProjectKlib) ?: fail("Failed loading klib: $providedTestProjectKlib")
        KotlinTestUtils.assertEqualsToFile(testDataDir.resolve("!testProject.addresses"), addresses.render())
    }
}