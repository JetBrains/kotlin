/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.wizard

import org.jetbrains.kotlin.tools.projectWizard.cli.BuildSystem
import org.jetbrains.kotlin.tools.projectWizard.cli.readSettingsYaml
import org.jetbrains.kotlin.tools.projectWizard.plugins.Plugins
import java.nio.file.Path

abstract class AbstractYamlNewWizardProjectImportTest : AbstractNewWizardProjectImportTest() {
    override fun createWizard(directory: Path, buildSystem: BuildSystem, projectDirectory: Path): Wizard {
        val yaml = readSettingsYaml(directory, buildSystem) ?: error("settings.yaml does not exists in $directory")
        return YamlWizard(
            yaml = yaml,
            projectPath = projectDirectory,
            createPlugins = Plugins.allPlugins,
            servicesManager = IDE_WIZARD_TEST_SERVICES_MANAGER,
            isUnitTestMode = true
        )
    }
}