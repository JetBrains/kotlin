/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import com.intellij.lang.html.HTMLLanguage
import com.intellij.testFramework.LightProjectDescriptor
import org.intellij.lang.annotations.Language
import org.intellij.lang.regexp.RegExpLanguage
import org.jetbrains.kotlin.idea.test.JdkAndMockLibraryProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.test.KotlinTestUtils

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
        return JdkAndMockLibraryProjectDescriptor(
                PluginTestCaseBase.getTestDataPathBase() + "/injection/lib/", false, false, false, true,
                arrayOf(KotlinTestUtils.getHomeDirectory() + "/ideaSDK/lib/annotations.jar"))
    }
}

