/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators


import org.jetbrains.kotlin.tools.projectWizard.KotlinNewProjectWizardBundle
import org.jetbrains.kotlin.tools.projectWizard.Versions
import org.jetbrains.kotlin.tools.projectWizard.core.Reader
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.ModuleConfiguratorSetting
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.ModuleConfiguratorSettingReference
import org.jetbrains.kotlin.tools.projectWizard.core.safeAs
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.irsList
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.maven.MavenOnlyPluginIR
import org.jetbrains.kotlin.tools.projectWizard.library.MavenArtifact
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.*
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.KotlinPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModulesToIrConversionData
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.DefaultRepository
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.ModuleKind

interface ModuleConfiguratorWithTests : ModuleConfiguratorWithSettings {
    companion object : ModuleConfiguratorSettings() {
        val testFramework by enumSetting<KotlinTestFramework>(
            KotlinNewProjectWizardBundle.message("module.configurator.tests.setting.framework"),
            neededAtPhase = GenerationPhase.PROJECT_GENERATION
        ) {
            filter = filter@{ reference, kotlinTestFramework ->
                if (reference !is ModuleConfiguratorSettingReference<*, *>) return@filter true

                val moduleType = reference.module?.configurator?.safeAs<ModuleConfiguratorWithModuleType>()?.moduleType
                moduleType in kotlinTestFramework.moduleTypes
            }
            defaultValue = dynamic { reference ->
                if (buildSystemType == BuildSystemType.Jps) return@dynamic KotlinTestFramework.NONE
                reference
                    .safeAs<ModuleConfiguratorSettingReference<*, *>>()
                    ?.module
                    ?.configurator
                    ?.safeAs<ModuleConfiguratorWithTests>()
                    ?.defaultTestFramework()
            }
        }
    }

    fun defaultTestFramework(): KotlinTestFramework

    override fun createModuleIRs(
        reader: Reader,
        configurationData: ModulesToIrConversionData,
        module: Module
    ): List<BuildSystemIR> = irsList {
        val testFramework = inContextOfModuleConfigurator(module) { reader { testFramework.reference.settingValue } }

        testFramework.dependencyNames.forEach { dependencyName ->
            +KotlinArbitraryDependencyIR(
                dependencyName,
                isInMppModule = module.kind
                    .let { it == ModuleKind.multiplatform || it == ModuleKind.target },
                kotlinVersion = reader { KotlinPlugin.version.propertyValue },
                dependencyType = DependencyType.TEST
            )
        }
        testFramework.additionalDependencies.forEach { +it }
    }

    override fun createBuildFileIRs(reader: Reader, configurationData: ModulesToIrConversionData, module: Module) = irsList {
        val testFramework = inContextOfModuleConfigurator(module) { reader { testFramework.reference.settingValue } }
        val buildSystemType = reader { buildSystemType }
        if (testFramework != KotlinTestFramework.NONE) {
            when {
                module.kind.isSingleplatform && buildSystemType.isGradle -> {
                    testFramework.usePlatform?.let { usePlatform ->
                        val testTaskAccess = if (buildSystemType == BuildSystemType.GradleKotlinDsl) "tasks.test" else "test"
                        testTaskAccess {
                            +"$usePlatform()"
                        }
                    }
                }
                buildSystemType == BuildSystemType.Maven -> {
                    +MavenOnlyPluginIR("maven-surefire-plugin", Versions.MAVEN_PLUGINS.SUREFIRE)
                    +MavenOnlyPluginIR("maven-failsafe-plugin", Versions.MAVEN_PLUGINS.FAILSAFE)
                }
            }
        }
    }

    override fun getConfiguratorSettings(): List<ModuleConfiguratorSetting<*, *>> = listOf(testFramework)
}

enum class KotlinTestFramework(
    override val text: String,
    val moduleTypes: List<ModuleType>,
    val usePlatform: String?,
    val dependencyNames: List<String>,
    val additionalDependencies: List<LibraryDependencyIR> = emptyList()
) : DisplayableSettingItem {
    NONE(
        KotlinNewProjectWizardBundle.message("module.configurator.tests.setting.framework.none"),
        ModuleType.ALL.toList(),
        usePlatform = null,
        emptyList()
    ),
    JUNIT4(
        KotlinNewProjectWizardBundle.message("module.configurator.tests.setting.framework.junit4"),
        listOf(ModuleType.jvm, ModuleType.android),
        usePlatform = "useJUnit",
        listOf("test-junit")
    ),
    JUNIT5(
        KotlinNewProjectWizardBundle.message("module.configurator.tests.setting.framework.junit5"),
        listOf(ModuleType.jvm),
        usePlatform = "useJUnitPlatform",
        dependencyNames = listOf("test-junit5"),
        additionalDependencies = listOf(
            ArtifactBasedLibraryDependencyIR(
                MavenArtifact(DefaultRepository.MAVEN_CENTRAL, "org.junit.jupiter", "junit-jupiter-api"),
                version = Versions.JUNIT5,
                dependencyType = DependencyType.TEST,
            ),
            ArtifactBasedLibraryDependencyIR(
                MavenArtifact(DefaultRepository.MAVEN_CENTRAL, "org.junit.jupiter", "junit-jupiter-engine"),
                version = Versions.JUNIT5,
                dependencyType = DependencyType.TEST,
                dependencyKind = DependencyKind.runtimeOnly
            ),
        )
    ),
    TEST_NG(
        KotlinNewProjectWizardBundle.message("module.configurator.tests.setting.framework.test.ng"),
        listOf(ModuleType.jvm),
        usePlatform = "useTestNG",
        listOf("test-testng")
    ),
    JS(
        KotlinNewProjectWizardBundle.message("module.configurator.tests.setting.framework.js"),
        listOf(ModuleType.js),
        usePlatform = null,
        listOf("test-js")
    ),
    COMMON(
        KotlinNewProjectWizardBundle.message("module.configurator.tests.setting.framework.common"),
        listOf(ModuleType.common),
        usePlatform = null,
        listOf("test-common", "test-annotations-common")
    )
}

val KotlinTestFramework.isPresent
    get() = this != KotlinTestFramework.NONE