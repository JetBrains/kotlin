/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import org.intellij.lang.annotations.Language
import org.intellij.lang.regexp.RegExpLanguage
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.junit.runner.RunWith

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
class KotlinStdlibInjectionTest : AbstractInjectionTest() {
    fun testOnRegex0() = assertInjectionPresent(
        """
            val test1 = Regex("<caret>some")
            """,
        RegExpLanguage.INSTANCE.id
    )

    fun testOnRegex1() = assertInjectionPresent(
        """
            val test1 = Regex("<caret>some", RegexOption.COMMENTS)
            """,
        RegExpLanguage.INSTANCE.id
    )

    fun testOnRegex2() = assertInjectionPresent(
        """
            val test1 = Regex("<caret>some", setOf(RegexOption.COMMENTS))
            """,
        RegExpLanguage.INSTANCE.id
    )

    fun testToRegex0() = assertInjectionPresent(
        """
            val test = "hi<caret>".toRegex()
            """,
        RegExpLanguage.INSTANCE.id
    )

    fun testToRegex1() = assertInjectionPresent(
        """
            val test = "hi<caret>".toRegex(RegexOption.CANON_EQ)
            """,
        RegExpLanguage.INSTANCE.id
    )

    fun testToRegex2() = assertInjectionPresent(
        """
            val test = "hi<caret>".toRegex(setOf(RegexOption.LITERAL))
            """,
        RegExpLanguage.INSTANCE.id
    )

    private fun assertInjectionPresent(@Language("kotlin") text: String, languageId: String) {
        doInjectionPresentTest(text, languageId = languageId, unInjectShouldBePresent = false)
    }

    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
}

