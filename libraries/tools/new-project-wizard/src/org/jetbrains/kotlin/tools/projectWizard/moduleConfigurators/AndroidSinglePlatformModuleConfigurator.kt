package org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators

import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.entity.ModuleConfiguratorSetting
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.AndroidConfigIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.BuildScriptDependencyIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.BuildScriptRepositoryIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.RawGradleIR
import org.jetbrains.kotlin.tools.projectWizard.library.MavenArtifact
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.gradle.GradlePlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleConfigurationData
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.plugins.templates.TemplatesPlugin
import org.jetbrains.kotlin.tools.projectWizard.settings.JavaPackage
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.DefaultRepository
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.settings.javaPackage
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version
import org.jetbrains.kotlin.tools.projectWizard.templates.FileTemplate
import org.jetbrains.kotlin.tools.projectWizard.templates.FileTemplateDescriptor
import java.nio.file.Path

object AndroidSinglePlatformModuleConfigurator : ModuleConfiguratorWithSettings(),
    SinglePlatformModuleConfigurator,
    AndroidModuleConfigurator {
    override val moduleType = ModuleType.jvm
    override val id = "android"
    override val suggestedModuleName = "android"
    override val greyText = "Requires Android SDK"

    val androidSdkPath by pathSetting("Android SDK Path", neededAtPhase = GenerationPhase.PROJECT_GENERATION) {
        shouldExists()
    }

    override fun createBuildFileIRs(configurationData: ModuleConfigurationData, module: Module) =
        buildList<BuildSystemIR> {
            +GradleOnlyPluginByNameIR("com.android.application")

            // it is explicitly here instead of by `createKotlinPluginIR` as it should be after `com.android.application`
            +KotlinBuildSystemPluginIR(
                KotlinBuildSystemPluginIR.Type.android,
                version = null // Version is already present in the parent buildfile
            )
            +GradleOnlyPluginByNameIR("kotlin-android-extensions")
            +AndroidConfigIR(configurationData.pomIr.artifactId)
            +listOf(
                DefaultRepository.GRADLE_PLUGIN_PORTAL,
                DefaultRepository.GOOGLE,
                DefaultRepository.JCENTER
            ).map(::RepositoryIR)
        }

    override fun createRootBuildFileIrs(configurationData: ModuleConfigurationData) = buildList<BuildSystemIR> {
        +listOf(
            DefaultRepository.GRADLE_PLUGIN_PORTAL,
            DefaultRepository.GOOGLE,
            DefaultRepository.JCENTER
        ).map { BuildScriptRepositoryIR(RepositoryIR(it)) }
        +listOf(
            RawGradleIR { call("classpath") { +"com.android.tools.build:gradle:3.2.1".quotified } },
            RawGradleIR {
                call("classpath") {
                    call("kotlin", forceBrackets = true) {
                        +"gradle-plugin".quotified
                        +", "
                        +configurationData.kotlinVersion.toString().quotified
                    }
                }
            }
        ).map(::BuildScriptDependencyIR)
    }

    override fun TaskRunningContext.runArbitraryTask(
        configurationData: ModuleConfigurationData,
        module: Module,
        modulePath: Path
    ): TaskResult<Unit> = withSettingsOf(module) {
        val path = androidSdkPath.reference.settingValue
        GradlePlugin::localProperties.addValues(
            "sdk.dir" to path
        )
    } andThen computeM {
        val javaPackage = module.javaPackage(configurationData.pomIr)
        TemplatesPlugin::addFileTemplates.execute(
            listOf(
                FileTemplate(FileTemplateDescriptors.activityMainXml, modulePath, "package" to javaPackage),
                FileTemplate(FileTemplateDescriptors.androidManifestXml, modulePath, "package" to javaPackage),
                FileTemplate(FileTemplateDescriptors.mainActivityKt(javaPackage), modulePath, "package" to javaPackage)
            )
        )
    }

    override fun createModuleIRs(configurationData: ModuleConfigurationData, module: Module): List<BuildSystemIR> =
        buildList {
            +ArtifactBasedLibraryDependencyIR(
                MavenArtifact(DefaultRepository.GOOGLE, "androidx.appcompat", "appcompat"),
                version = Version.fromString("1.1.0"),
                dependencyType = DependencyType.MAIN
            )

            +ArtifactBasedLibraryDependencyIR(
                MavenArtifact(DefaultRepository.GOOGLE, "androidx.core", "core-ktx"),
                version = Version.fromString("1.1.0"),
                dependencyType = DependencyType.MAIN
            )

            +KotlinLibraryDependencyIR("stdlib-jdk7", configurationData.kotlinVersion, DependencyType.MAIN)

            +ArtifactBasedLibraryDependencyIR(
                MavenArtifact(DefaultRepository.GOOGLE, "androidx.constraintlayout", "constraintlayout"),
                version = Version.fromString("1.1.3"),
                dependencyType = DependencyType.MAIN
            )
        }

    override val settings: List<ModuleConfiguratorSetting<*, *>> =
        listOf(androidSdkPath)

    private object FileTemplateDescriptors {
        val activityMainXml = FileTemplateDescriptor(
            "android/activity_main.xml.vm",
            "src" / "main" / "res" / "layout" / "activity_main.xml"
        )

        val androidManifestXml = FileTemplateDescriptor(
            "android/AndroidManifest.xml.vm",
            "src" / "main" / "AndroidManifest.xml"
        )

        fun mainActivityKt(javaPackage: JavaPackage) = FileTemplateDescriptor(
            "android/MainActivity.kt.vm",
            "src" / "main" / "java" / javaPackage.asPath() / "MainActivity.kt"
        )
    }
}