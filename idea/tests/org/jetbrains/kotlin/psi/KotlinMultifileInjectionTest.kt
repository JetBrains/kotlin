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

import com.intellij.lang.html.HTMLLanguage
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase

class KotlinMultiFileInjectionTest : AbstractInjectionTest() {
    public override fun setUp() {
        super.setUp()
        myFixture.testDataPath = PluginTestCaseBase.getTestDataPathBase() + "/injection"
    }

    fun testInjectionOfCustomParameterInJavaConstructorWithAnnotation() = doMultiFileInjectionPresentTest(HTMLLanguage.INSTANCE.id, false)

    fun testInjectionOfCustomParameterJavaWithAnnotation() = doMultiFileInjectionPresentTest(HTMLLanguage.INSTANCE.id, false)

    override fun getTestDataPath(): String? {
        return "idea/testData/injection/"
    }

    private fun doMultiFileInjectionPresentTest(languageId: String? = null, unInjectShouldBePresent: Boolean = true) {
        val testName = getTestName(false)

        myFixture.configureByFile("$testName.java")
        myFixture.configureByFile("$testName.kt")

        assertInjectionPresent(languageId, unInjectShouldBePresent)
    }
}

