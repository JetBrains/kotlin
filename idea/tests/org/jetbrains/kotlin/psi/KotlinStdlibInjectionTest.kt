/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.psi

import org.intellij.lang.annotations.Language
import org.intellij.lang.regexp.RegExpLanguage
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor

class KotlinStdlibInjectionTest : AbstractInjectionTest() {
    fun testOnRegex0() = assertInjectionPresent(
            """
            val test1 = kotlin.text.Regex("<caret>some")
            """,
            RegExpLanguage.INSTANCE.id
    )

    fun testOnRegex1() = assertInjectionPresent(
            """
            val test1 = kotlin.text.Regex("<caret>some", RegexOption.COMMENTS)
            """,
            RegExpLanguage.INSTANCE.id
    )

    fun testOnRegex2() = assertInjectionPresent(
            """
            val test1 = kotlin.text.Regex("<caret>some", setOf(RegexOption.COMMENTS))
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

