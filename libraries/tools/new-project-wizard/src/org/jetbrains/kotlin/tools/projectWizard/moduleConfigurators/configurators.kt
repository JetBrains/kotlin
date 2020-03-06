package org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators

import org.jetbrains.kotlin.tools.projectWizard.core.context.ReadingContext
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.ModuleConfiguratorSetting
import org.jetbrains.kotlin.tools.projectWizard.core.safeAs
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.BuildSystemIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.KotlinBuildSystemPluginIR
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleConfigurationData
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.ModuleKind
import org.jetbrains.kotlin.tools.projectWizard.core.buildList
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.*
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.maven.MavenPropertyIR
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemType
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.isGradle

interface JvmModuleConfigurator : ModuleConfiguratorWithTests {
    companion object : ModuleConfiguratorSettings() {
        val targetJvmVersion by enumSetting<TargetJvmVersion>("Target JVM Version", GenerationPhase.PROJECT_GENERATION) {
            defaultValue = value(TargetJvmVersion.JVM_1_8)
        }
    }

    override fun getConfiguratorSettings(): List<ModuleConfiguratorSetting<*, *>> = buildList {
        +super.getConfiguratorSettings()
        +targetJvmVersion
    }
}

enum class TargetJvmVersion(val value: String) : DisplayableSettingItem {
    JVM_1_6("1.6"),
    JVM_1_8("1.8"),
    JVM_9("9"),
    JVM_10("10"),
    JVM_11("11"),
    JVM_12("12"),
    JVM_13("13");

    override val text: String
        get() = value
}


interface ModuleConfiguratorWithModuleType : ModuleConfigurator {
    val moduleType: ModuleType
}

val ModuleConfigurator.moduleType: ModuleType?
    get() = safeAs<ModuleConfiguratorWithModuleType>()?.moduleType

object MppModuleConfigurator : ModuleConfigurator {
    override val moduleKind = ModuleKind.multiplatform
    override val suggestedModuleName = "shared"
    override val id = "multiplatform"
    override val text = "Multiplatform"
    override val canContainSubModules = true

    override fun createKotlinPluginIR(configurationData: ModuleConfigurationData, module: Module): KotlinBuildSystemPluginIR? =
        KotlinBuildSystemPluginIR(
            KotlinBuildSystemPluginIR.Type.multiplatform,
            version = configurationData.kotlinVersion
        )
}


interface SinglePlatformModuleConfigurator : ModuleConfigurator {
    val needCreateBuildFile: Boolean get() = true
}

object JvmSinglePlatformModuleConfigurator : JvmModuleConfigurator,
    SinglePlatformModuleConfigurator,
    ModuleConfiguratorWithModuleType {
    override val moduleType get() = ModuleType.jvm
    override val moduleKind: ModuleKind get() = ModuleKind.singleplatformJvm
    override val suggestedModuleName = "jvm"
    override val id = "JVM Module"

    override fun defaultTestFramework(): KotlinTestFramework = KotlinTestFramework.JUNIT4

    override val canContainSubModules = true

    override fun createKotlinPluginIR(configurationData: ModuleConfigurationData, module: Module): KotlinBuildSystemPluginIR? =
        KotlinBuildSystemPluginIR(
            KotlinBuildSystemPluginIR.Type.jvm,
            version = configurationData.kotlinVersion
        )


    override fun createBuildFileIRs(
        readingContext: ReadingContext,
        configurationData: ModuleConfigurationData,
        module: Module
    ): List<BuildSystemIR> =
        buildList {
            +GradleImportIR("org.jetbrains.kotlin.gradle.tasks.KotlinCompile")

            val targetVersionValue = withSettingsOf(module) {
                with(readingContext) {
                    JvmModuleConfigurator.targetJvmVersion.reference.settingValue.value
                }
            }
            when {
                configurationData.buildSystemType.isGradle -> {
                    +GradleConfigureTaskIR(
                        GradleByClassTasksAccessIR("KotlinCompile"),
                        irs = listOf(
                            GradleAssignmentIR("kotlinOptions.jvmTarget", GradleStringConstIR(targetVersionValue))
                        )
                    )
                }
                configurationData.buildSystemType == BuildSystemType.Maven -> {
                    +MavenPropertyIR("kotlin.compiler.jvmTarget", targetVersionValue)
                }
            }
        }
}





val ModuleType.defaultTarget
    get() = when (this) {
        ModuleType.jvm -> JvmTargetConfigurator
        ModuleType.js -> JsBrowserTargetConfigurator
        ModuleType.native -> NativeForCurrentSystemTarget
        ModuleType.common -> CommonTargetConfigurator
        ModuleType.android -> AndroidTargetConfigurator
    }