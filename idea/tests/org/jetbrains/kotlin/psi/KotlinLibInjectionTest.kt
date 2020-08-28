/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.html.HTMLLanguage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.LightProjectDescriptor
import org.intellij.lang.annotations.Language
import org.intellij.lang.regexp.RegExpLanguage
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.test.SdkAndMockLibraryProjectDescriptor
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.junit.runner.RunWith
import java.io.File
import java.net.URLClassLoader

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
class KotlinLibInjectionTest : AbstractInjectionTest() {
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
        val urls = (ApplicationManager::class.java.classLoader as? URLClassLoader)?.urLs
            ?: error("Can't find path urls in classloader")

        val libName = "annotations.jar"
        val libCandidates = urls
            .map { it.path.replace("\\", "/") }
            .filter { it.endsWith("/$libName") }

        val libPath = libCandidates.singleOrNull()
            ?: error("Can't find single $libName in classpath among $libCandidates")

        val libFile = File(libPath).also { file ->
            require(file.exists()) { "Can't find library: ${file.absolutePath}" }
        }

        return SdkAndMockLibraryProjectDescriptor(
            PluginTestCaseBase.getTestDataPathBase() + "/injection/lib/", false, false, false, true,
            listOf(libFile.absolutePath)
        )
    }
}
