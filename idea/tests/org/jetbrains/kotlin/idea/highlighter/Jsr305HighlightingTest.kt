/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.resolve.JvmTarget
import org.jetbrains.kotlin.config.KotlinFacetSettingsProvider
import org.jetbrains.kotlin.idea.stubs.createFacet
import org.jetbrains.kotlin.idea.test.KotlinJdkAndLibraryProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.platform.impl.JvmIdePlatformKind
import org.jetbrains.kotlin.resolve.DefaultBuiltInPlatforms
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
                module.createFacet(DefaultBuiltInPlatforms.jvm18)
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
