/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.cli

import org.jetbrains.kotlin.tools.projectWizard.plugins.Plugins
import org.jetbrains.kotlin.tools.projectWizard.wizard.Wizard
import org.jetbrains.kotlin.tools.projectWizard.wizard.YamlWizard
import java.nio.file.Path

abstract class AbstractYamlBuildFileGenerationTest : AbstractBuildFileGenerationTest() {
    override fun createWizard(directory: Path, buildSystem: BuildSystem, projectDirectory: Path): Wizard {
        val yaml = readSettingsYaml(directory, buildSystem) ?: error("settings.yaml should exists in $directory")
        return YamlWizard(
            yaml,
            projectDirectory,
            Plugins.allPlugins,
            isUnitTestMode = true,
            servicesManager = CLI_WIZARD_TEST_SERVICES_MANAGER
        )
    }
}