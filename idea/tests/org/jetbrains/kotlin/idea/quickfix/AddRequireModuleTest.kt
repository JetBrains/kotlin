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
/*
package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.daemon.QuickFixBundle
import org.jetbrains.kotlin.idea.test.KotlinLightJava9ModulesCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinMultiModuleJava9ProjectDescriptor.ModuleDescriptor.*


class KotlinAddRequiredModuleTest : KotlinLightJava9ModulesCodeInsightFixtureTestCase() {
    private val messageM2 = QuickFixBundle.message("module.info.add.requires.name", "M_TWO")!!

    override fun setUp() {
        super.setUp()
        moduleInfo("module M_TWO { exports pkgA; }", M2)
        addJavaFile("pkgA/A.java", "package pkgA; public class A {}", M2)
    }

    fun testAddRequiresToModuleInfo() {
        moduleInfo("module MAIN {}", MAIN)
        val editedFile = addKotlinFile(
                "pkgB/B.kt",
                """
                package pkgB
                import pkgA.A
                class B(a: /*|*/A)
                """,
                MAIN)
        myFixture.configureFromExistingVirtualFile(editedFile)

        findActionAndExecute(messageM2)
        assertNoErrors()

        checkModuleInfo(
                """
                module MAIN {
                    requires M_TWO;
                }
                """)
    }

    fun testNoIdeaModuleDependency() {
        moduleInfo("module M_THREE {}", M3)
        val editedFile = addKotlinFile(
                "pkgB/B.kt",
                """
                package pkgB
                import pkgA.A
                class B(a: /*|*/A)
                """,
                M3)
        myFixture.configureFromExistingVirtualFile(editedFile)

        val actions = myFixture.filterAvailableIntentions(messageM2)
        assertEmpty(actions)
    }

    fun testAddRequiresToInfoForJavaModule() {
        moduleInfo("module MAIN {}", MAIN)
        val editedFile = addKotlinFile(
                "test.kt",
                """
                fun test() {
                    java.util.logging./*|*/FileHandler()   // <-- error; "add 'requires java.logging'" quick fix expected
                }
                """,
                MAIN)
        myFixture.configureFromExistingVirtualFile(editedFile)

        findActionAndExecute(QuickFixBundle.message("module.info.add.requires.name", "java.logging")!!)

        assertNoErrors()
        checkModuleInfo(
                """
                module MAIN {
                    requires java.logging;
                }
                """)
    }

    private fun assertNoErrors() {
        myFixture.checkHighlighting(false, false, false)
    }

    private fun findActionAndExecute(message: String) {
        val action = myFixture.findSingleIntention(message)
        assertNotNull(action)
        myFixture.launchAction(action)
    }
}
*/