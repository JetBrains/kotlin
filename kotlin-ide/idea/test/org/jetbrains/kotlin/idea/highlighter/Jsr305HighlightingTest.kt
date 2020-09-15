/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.config.KotlinFacetSettingsProvider
import org.jetbrains.kotlin.idea.artifacts.KotlinArtifacts
import org.jetbrains.kotlin.idea.stubs.createFacet
import org.jetbrains.kotlin.idea.test.IDEA_TEST_DATA_DIR
import org.jetbrains.kotlin.idea.test.KotlinJdkAndLibraryProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.*
import org.jetbrains.kotlin.utils.ReportLevel
import org.junit.internal.runners.JUnit38ClassRunner
import org.junit.runner.RunWith
import java.io.File

@TestRoot("idea")
@TestMetadata("testData/highlighterJsr305/project")
@RunWith(JUnit38ClassRunner::class)
class Jsr305HighlightingTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor(): LightProjectDescriptor {
        val foreignAnnotationsJar = File(PathManager.getCommunityHomePath() + "/lib/jsr305.jar")
        val libraryJar = KotlinCompilerStandalone(
            listOf(IDEA_TEST_DATA_DIR.resolve("highlighterJsr305/library")),
            classpath = listOf(foreignAnnotationsJar)
        ).compile()

        return object : KotlinJdkAndLibraryProjectDescriptor(
            listOf(
                KotlinArtifacts.instance.kotlinStdlib,
                foreignAnnotationsJar,
                libraryJar
            )
        ) {
            override fun configureModule(module: Module, model: ModifiableRootModel) {
                super.configureModule(module, model)
                module.createFacet(JvmPlatforms.jvm18)
                val facetSettings = KotlinFacetSettingsProvider.getInstance(module.project)?.getInitializedSettings(module)

                facetSettings?.apply {
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
}
