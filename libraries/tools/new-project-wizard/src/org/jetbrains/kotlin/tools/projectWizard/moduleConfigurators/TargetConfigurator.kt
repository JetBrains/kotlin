package org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators

import kotlinx.collections.immutable.toPersistentList
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.tools.projectWizard.KotlinNewProjectWizardBundle
import org.jetbrains.kotlin.tools.projectWizard.core.Reader

import org.jetbrains.kotlin.tools.projectWizard.core.buildList
import org.jetbrains.kotlin.tools.projectWizard.core.buildPersistenceList
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.ModuleConfiguratorSetting
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.BuildSystemIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.*
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.multiplatform.DefaultTargetConfigurationIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.multiplatform.TargetAccessIR
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.buildSystemType
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.isGradle
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleSubType
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.ModuleKind


interface TargetConfigurator : ModuleConfiguratorWithModuleType {
    override val moduleKind get() = ModuleKind.target

    fun canCoexistsWith(other: List<TargetConfigurator>): Boolean = true

    fun Reader.createTargetIrs(module: Module): List<BuildSystemIR>
    fun createInnerTargetIrs(
        reader: Reader,
        module: Module
    ): List<BuildSystemIR> = emptyList()
}

abstract class TargetConfiguratorWithTests : ModuleConfiguratorWithTests, TargetConfigurator

interface SingleCoexistenceTargetConfigurator : TargetConfigurator {
    override fun canCoexistsWith(other: List<TargetConfigurator>): Boolean =
        other.none { it == this }
}

interface SimpleTargetConfigurator : TargetConfigurator {
    val moduleSubType: ModuleSubType
    override val moduleType get() = moduleSubType.moduleType
    override val id get() = "${moduleSubType.name}Target"

    override val suggestedModuleName: String? get() = moduleSubType.name


    override fun Reader.createTargetIrs(
        module: Module
    ): List<BuildSystemIR> = buildList {
        +DefaultTargetConfigurationIR(
            module.createTargetAccessIr(moduleSubType),
            createInnerTargetIrs(this@createTargetIrs, module).toPersistentList()
        )
    }
}

private fun Module.createTargetAccessIr(moduleSubType: ModuleSubType) =
    TargetAccessIR(
        moduleSubType,
        name.takeIf { it != moduleSubType.name }
    )


interface JsTargetConfigurator : JSConfigurator, TargetConfigurator, SingleCoexistenceTargetConfigurator

object JsBrowserTargetConfigurator : JsTargetConfigurator, ModuleConfiguratorWithTests {
    @NonNls
    override val id = "jsBrowser"

    @NonNls
    override val suggestedModuleName = "browser"

    override val text = KotlinNewProjectWizardBundle.message("module.configurator.js.browser")


    override fun defaultTestFramework(): KotlinTestFramework = KotlinTestFramework.JS

    override fun Reader.createTargetIrs(
        module: Module
    ): List<BuildSystemIR> = buildList {
        +DefaultTargetConfigurationIR(
            module.createTargetAccessIr(ModuleSubType.js),
            buildPersistenceList {
                +RawGradleIR {
                    sectionCall("browser") {}
                }
            }
        )
    }
}

object JsNodeTargetConfigurator : JsTargetConfigurator {
    @NonNls
    override val id = "jsNode"

    @NonNls
    override val suggestedModuleName = "nodeJs"

    override val text = KotlinNewProjectWizardBundle.message("module.configurator.js.node")


    override fun Reader.createTargetIrs(
        module: Module
    ): List<BuildSystemIR> = buildList {
        +DefaultTargetConfigurationIR(
            module.createTargetAccessIr(ModuleSubType.js),
            buildPersistenceList {
                +RawGradleIR {
                    sectionCall("nodejs") {}
                }
            }
        )
    }
}

object CommonTargetConfigurator : TargetConfiguratorWithTests(), SimpleTargetConfigurator, SingleCoexistenceTargetConfigurator {
    override val moduleSubType = ModuleSubType.common
    override val text: String = KotlinNewProjectWizardBundle.message("module.configurator.common")

    override fun defaultTestFramework(): KotlinTestFramework = KotlinTestFramework.COMMON
}

object JvmTargetConfigurator : JvmModuleConfigurator,
    TargetConfigurator,
    SimpleTargetConfigurator {
    override val moduleSubType = ModuleSubType.jvm

    override val text: String = KotlinNewProjectWizardBundle.message("module.configurator.jvm")

    override fun defaultTestFramework(): KotlinTestFramework = KotlinTestFramework.JUNIT4

    override fun createInnerTargetIrs(
        reader: Reader,
        module: Module
    ): List<BuildSystemIR> = buildList {
        reader {
            withSettingsOf(module) {
                val targetVersionValue = JvmModuleConfigurator.targetJvmVersion.reference.settingValue.value
                if (buildSystemType.isGradle) {
                    +GradleSectionIR(
                        "compilations.all",
                        BodyIR(
                            GradleAssignmentIR("kotlinOptions.jvmTarget", GradleStringConstIR(targetVersionValue))
                        )
                    )
                }
                if (Settings.javaSupport.reference.settingValue) {
                    +GradleCallIr("withJava")
                }
            }
        }
    }

    override fun getConfiguratorSettings(): List<ModuleConfiguratorSetting<*, *>> =
        super.getConfiguratorSettings() +
                Settings.javaSupport

    object Settings : ModuleConfiguratorSettings() {
        val javaSupport by booleanSetting(
            KotlinNewProjectWizardBundle.message("module.configurator.jvm.setting.java.support"),
            GenerationPhase.PROJECT_GENERATION
        ) {
            defaultValue = value(false)
        }
    }
}
