/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.wizard

import org.jetbrains.kotlin.tools.projectWizard.cli.BuildSystem
import org.jetbrains.plugins.gradle.settings.DistributionType
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

class ScriptHighlightingGradleDistributionTypeTest : AbstractProjectTemplateNewWizardProjectImportTest() {

    @Test
    fun testScriptHighlightingGradleWrapped() {
        doTest(DistributionType.WRAPPED)
    }

    @Test
    fun testScriptHighlightingGradleDefaultWrapped() {
        doTest(DistributionType.DEFAULT_WRAPPED)
    }

    @Test
    fun testScriptHighlightingGradleBundled() {
        doTest(DistributionType.BUNDLED)
    }

    private fun doTest(distributionType: DistributionType) {
        val directory = Paths.get("backendApplication")
        val tempDirectory = Files.createTempDirectory(null)

        prepareGradleBuildSystem(tempDirectory, distributionType)

        runWizard(directory, BuildSystem.GRADLE_KOTLIN_DSL, tempDirectory)

        checkScriptConfigurationsIfAny()
    }
}