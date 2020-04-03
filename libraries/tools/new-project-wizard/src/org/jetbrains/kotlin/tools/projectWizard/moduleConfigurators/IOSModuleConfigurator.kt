/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators

import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.tools.projectWizard.KotlinNewProjectWizardBundle
import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModulesToIrConversionData
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.templates.FileTemplate
import org.jetbrains.kotlin.tools.projectWizard.templates.FileTemplateDescriptor
import java.nio.file.Path

object IOSSinglePlatformModuleConfigurator : SinglePlatformModuleConfigurator, ModuleConfiguratorSettings() {
    @NonNls
    override val id = "IOS Module"

    @NonNls
    override val suggestedModuleName = "ios"

    override val moduleKind: ModuleKind = ModuleKind.singleplatformJvm
    override val greyText = KotlinNewProjectWizardBundle.message("module.configurator.ios.requires.xcode")
    override val text = KotlinNewProjectWizardBundle.message("module.configurator.ios")


    override val needCreateBuildFile: Boolean = false
    override val requiresRootBuildFile: Boolean = true

    override fun Reader.createTemplates(
        configurationData: ModulesToIrConversionData,
        module: Module,
        modulePath: Path
    ): List<FileTemplate> {
        val settings = createTemplatesSettingValues(module)

        fun fileTemplate(path: Path) = FileTemplate(descriptor(path, module.name), modulePath, settings)

        return buildList {
            +fileTemplate("$DEFAULT_APP_NAME.xcodeproj" / "project.pbxproj")

            +fileTemplate(DEFAULT_APP_NAME / "AppDelegate.swift")
            +fileTemplate(DEFAULT_APP_NAME / "ContentView.swift")
            +fileTemplate(DEFAULT_APP_NAME / "SceneDelegate.swift")
            +fileTemplate(DEFAULT_APP_NAME / "Info.plist")

            +fileTemplate(DEFAULT_APP_NAME / "Base.lproj" / "LaunchScreen.storyboard")

            +fileTemplate(DEFAULT_APP_NAME / "Assets.xcassets" / "Contents.json")
            +fileTemplate(DEFAULT_APP_NAME / "Assets.xcassets" / "AppIcon.appiconset" / "Contents.json")
            +fileTemplate(DEFAULT_APP_NAME / "Preview_Content" / "Preview_Assets.xcassets" / "Contents.json")

            +fileTemplate("${DEFAULT_APP_NAME}Tests" / "Info.plist")
            +fileTemplate("${DEFAULT_APP_NAME}Tests" / "${DEFAULT_APP_NAME}Tests.swift")

            +fileTemplate("${DEFAULT_APP_NAME}UITests" / "Info.plist")
            +fileTemplate("${DEFAULT_APP_NAME}UITests" / "${DEFAULT_APP_NAME}UITests.swift")
        }
    }

    private fun Reader.createTemplatesSettingValues(module: Module): Map<String, Any?> {
        val dependentModule = withSettingsOf(module) {
            dependentModule.reference.notRequiredSettingValue?.module
        }

        return mapOf(
            "moduleName" to module.name,
            "sharedModuleName" to dependentModule?.name
        )
    }

    private fun descriptor(path: Path, moduleName: String) =
        FileTemplateDescriptor(
            "ios/singleplatformProject/$path",
            path.toString()
                .removeSuffix(".vm")
                .replace(DEFAULT_APP_NAME, moduleName)
                .replace('_', ' ')
                .asPath()
        )

    @NonNls
    private const val DEFAULT_APP_NAME = "appName"

    val dependentModule by valueSetting<DependentModuleReference>(
        "",
        GenerationPhase.PROJECT_GENERATION,
        alwaysFailingParser("Dependent module setting should not be parsed")
    ) {
        defaultValue = value(DependentModuleReference.EMPTY)
    }

    data class DependentModuleReference(val module: Module?) {
        companion object {
            val EMPTY = DependentModuleReference(module = null)
        }
    }
}