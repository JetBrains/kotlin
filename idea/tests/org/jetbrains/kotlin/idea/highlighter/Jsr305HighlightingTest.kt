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

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.KotlinFacetSettingsProvider
import org.jetbrains.kotlin.idea.stubs.createFacet
import org.jetbrains.kotlin.idea.test.KotlinJdkAndLibraryProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.platform.impl.JvmIdePlatformKind
import org.jetbrains.kotlin.test.MockLibraryUtil
import org.jetbrains.kotlin.utils.ReportLevel

class Jsr305HighlightingTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor(): LightProjectDescriptor {
        val foreignAnnotationsJar = MockLibraryUtil.compileJvmLibraryToJar("third-party/annotations", "foreign-annotations")
        val libraryJar = MockLibraryUtil.compileJvmLibraryToJar("idea/testData/highlighterJsr305/library", "jsr305-library",
                                                                extraClasspath = listOf(foreignAnnotationsJar.absolutePath))
        return object : KotlinJdkAndLibraryProjectDescriptor(
                listOf(
                        ForTestCompileRuntime.runtimeJarForTests(),
                        foreignAnnotationsJar,
                        libraryJar
                )
        ) {
            override fun configureModule(module: Module, model: ModifiableRootModel) {
                super.configureModule(module, model)
                module.createFacet(JvmIdePlatformKind.Platform(JvmTarget.JVM_1_8))
                val facetSettings = KotlinFacetSettingsProvider.getInstance(project).getInitializedSettings(module)

                facetSettings.apply {
                    val jsrStateByTestName =
                            ReportLevel.findByDescription(getTestName(true)) ?: return@apply

                    compilerSettings!!.additionalArguments += " -Xjsr305=${jsrStateByTestName.description}"
                    updateMergedArguments()
                }
            }
        }
    }

    fun testIgnore() {
        doTest()
    }

    fun testWarn() {
        doTest()
    }

    fun testStrict() {
        doTest()
    }

    fun testDefault() {
        doTest()
    }

    private fun doTest() {
        myFixture.configureByFile("${getTestName(false)}.kt")
        myFixture.checkHighlighting()
    }

    override fun getTestDataPath() = "idea/testData/highlighterJsr305/project"
}
