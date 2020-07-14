/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.html.HTMLLanguage
import org.intellij.lang.annotations.Language
import org.intellij.lang.regexp.RegExpLanguage
import org.jetbrains.kotlin.idea.test.MockLibraryFacility
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.junit.internal.runners.JUnit38ClassRunner
import org.junit.runner.RunWith
import java.io.File

@RunWith(JUnit38ClassRunner::class)
class KotlinLibInjectionTest : AbstractInjectionTest() {
    private val mockLibraryFacility = MockLibraryFacility(
        File(PluginTestCaseBase.getTestDataPathBase(), "injection/lib"),
        attachSources = false
    )

    fun testFunInjection() = assertInjectionPresent(
        """
            import injection.html
            fun test() {
                12.html("<caret><html></html>")
            }
            """,
        HTMLLanguage.INSTANCE.id
    )

    fun testFunInjectionWithImportedAnnotation() = assertInjectionPresent(
        """
            import injection.regexp
            fun test() {
                12.regexp("<caret>test")
            }
            """,
        RegExpLanguage.INSTANCE.id
    )

    private fun assertInjectionPresent(@Language("kotlin") text: String, languageId: String) {
        doInjectionPresentTest(text, languageId = languageId, unInjectShouldBePresent = false)
    }

    override fun setUp() {
        super.setUp()
        mockLibraryFacility.setUp(module)
    }

    override fun tearDown() {
        mockLibraryFacility.tearDown(module)
        super.tearDown()
    }
}
