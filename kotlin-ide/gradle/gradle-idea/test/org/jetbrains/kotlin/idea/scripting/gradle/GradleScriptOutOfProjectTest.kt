/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager
import org.jetbrains.kotlin.idea.script.AbstractScriptConfigurationLoadingTest
import org.jetbrains.kotlin.idea.scripting.gradle.legacy.GradleLegacyScriptConfigurationLoaderForOutOfProjectScripts
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.junit.internal.runners.JUnit38ClassRunner
import org.junit.runner.RunWith
import java.io.File

@RunWith(JUnit38ClassRunner::class)
class GradleScriptOutOfProjectTest : AbstractScriptConfigurationLoadingTest() {

    override fun setUpTestProject() {
        val rootDir = File(KotlinTestUtils.getHomeDirectory(), "idea/testData/script/definition/loading/gradle/")

        val buildGradleKts = rootDir.walkTopDown().find { it.name == GradleConstants.KOTLIN_DSL_SCRIPT_NAME }
            ?: error("Couldn't find main script")

        configureScriptFile(rootDir, buildGradleKts)

        testAffectedGradleProjectFiles = true
    }

    override fun tearDown() {
        super.tearDown()

        testAffectedGradleProjectFiles = false
    }

    private val loaderForOutOfProjectScripts by lazy {
        GradleLegacyScriptConfigurationLoaderForOutOfProjectScripts(
            project
        )
    }

    fun testManualLoadingForUpToDate() {
        assertConfigurationShouldBeLoadedManually()

        scriptConfigurationManager.default.forceReloadConfiguration(myFile as KtFile, loaderForOutOfProjectScripts)

        assertConfigurationWasLoaded()
    }

    fun testManualLoadingForOutOfDate() {
        assertConfigurationShouldBeLoadedManually()

        makeChangesInsideSections()

        assertNoBackgroundTasks()
        assertNoLoading()

        scriptConfigurationManager.default.forceReloadConfiguration(myFile as KtFile, loaderForOutOfProjectScripts)

        assertConfigurationWasLoaded()
    }

    fun testFileAttributesForManualLoading() {
        assertConfigurationShouldBeLoadedManually()

        scriptConfigurationManager.default.forceReloadConfiguration(myFile as KtFile, loaderForOutOfProjectScripts)

        assertConfigurationWasLoaded()

        ScriptConfigurationManager.clearCaches(project)

        assertNotNull(getConfiguration())
    }

    fun testNoNotificationAfterForReload() {
        assertConfigurationShouldBeLoadedManually()

        scriptConfigurationManager.default.forceReloadConfiguration(myFile as KtFile, loaderForOutOfProjectScripts)
        assertConfigurationWasLoaded()

        makeChangesInsideSections()

        scriptConfigurationManager.default.forceReloadConfiguration(myFile as KtFile, loaderForOutOfProjectScripts)
        assertConfigurationWasLoaded()

    }

    private fun makeChangesInsideSections() {
        changeContents(myFile.text.replace(GradleScriptListenerTest.insidePlaceholder, "application"), myFile)
    }

    private fun assertConfigurationShouldBeLoadedManually() {
        assertNull(getConfiguration())
        assertNoLoading()
    }

    private fun assertConfigurationWasLoaded() {
        assertAndDoAllBackgroundTasks()
        assertSingleLoading()
        assertNoSuggestedConfiguration()
        assertNotNull(getConfiguration())
    }

    private fun getConfiguration() =
        scriptConfigurationManager.getConfiguration(myFile as KtFile)
}
