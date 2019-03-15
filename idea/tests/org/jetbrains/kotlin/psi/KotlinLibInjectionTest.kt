/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.html.HTMLLanguage
import com.intellij.testFramework.LightProjectDescriptor
import org.intellij.lang.annotations.Language
import org.intellij.lang.regexp.RegExpLanguage
import org.jetbrains.kotlin.idea.test.SdkAndMockLibraryProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import java.io.File

class KotlinLibInjectionTest : AbstractInjectionTest() {
    override fun setUp() {
        super.setUp()
    }

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


    override fun getProjectDescriptor(): LightProjectDescriptor {
        val ideaSdkPath = System.getProperty("idea.home.path")?.takeIf { File(it).isDirectory }
                          ?: throw RuntimeException("Unable to get a valid path from 'idea.home.path' property, please point it to the Idea location")
        return SdkAndMockLibraryProjectDescriptor(
                PluginTestCaseBase.getTestDataPathBase() + "/injection/lib/", false, false, false, true,
                listOf(File(ideaSdkPath, "lib/annotations.jar").absolutePath))
    }
}
