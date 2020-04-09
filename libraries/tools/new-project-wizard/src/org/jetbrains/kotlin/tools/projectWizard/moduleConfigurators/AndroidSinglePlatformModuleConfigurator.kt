package org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators


import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.tools.projectWizard.KotlinNewProjectWizardBundle
import org.jetbrains.kotlin.tools.projectWizard.Versions
import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.library.MavenArtifact
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModulesToIrConversionData
import org.jetbrains.kotlin.tools.projectWizard.plugins.templates.TemplatesPlugin
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.DefaultRepository
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.ModuleKind
import org.jetbrains.kotlin.tools.projectWizard.settings.javaPackage
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version
import org.jetbrains.kotlin.tools.projectWizard.templates.FileTemplate
import java.nio.file.Path

object AndroidSinglePlatformModuleConfigurator :
    SinglePlatformModuleConfigurator,
    AndroidModuleConfigurator {
    override val moduleKind: ModuleKind get() = ModuleKind.singleplatformAndroid
    @NonNls
    override val id = "android"
    @NonNls
    override val suggestedModuleName = "android"
    override val text = KotlinNewProjectWizardBundle.message("module.configurator.android")

    override val requiresRootBuildFile: Boolean = true

    override fun createBuildFileIRs(
        reader: Reader,
        configurationData: ModulesToIrConversionData,
        module: Module
    ) = buildList<BuildSystemIR> {
        +super<AndroidModuleConfigurator>.createBuildFileIRs(reader, configurationData, module)

        // it is explicitly here instead of by `createKotlinPluginIR` as it should be after `com.android.application`
        +KotlinBuildSystemPluginIR(
            KotlinBuildSystemPluginIR.Type.android,
            version = configurationData.kotlinVersion
        )
    }.sortedBy { ir: BuildSystemIR ->
        if (ir is GradleOnlyPluginByNameIR) {
            // TODO implement proper sort on irs
            // But for now kotlin-android-extensions should be after Android kotlin plugin
            if (ir.pluginId == "kotlin-android-extensions") 1 else 0
        } else 0
    }

    override fun createModuleIRs(
        reader: Reader,
        configurationData: ModulesToIrConversionData,
        module: Module
    ) = buildList<BuildSystemIR> {
        +super<AndroidModuleConfigurator>.createModuleIRs(reader, configurationData, module)

        +ArtifactBasedLibraryDependencyIR(
            MavenArtifact(DefaultRepository.GOOGLE, "androidx.appcompat", "appcompat"),
            version = Versions.ANDROID.ANDROIDX_APPCOMPAT,
            dependencyType = DependencyType.MAIN
        )

        +ArtifactBasedLibraryDependencyIR(
            MavenArtifact(DefaultRepository.GOOGLE, "androidx.constraintlayout", "constraintlayout"),
            version = Versions.ANDROID.ANDROIDX_CONSTRAINTLAYOUT,
            dependencyType = DependencyType.MAIN
        )
    }

    override fun Writer.runArbitraryTask(
        configurationData: ModulesToIrConversionData,
        module: Module,
        modulePath: Path
    ): TaskResult<Unit> = computeM {
        val javaPackage = module.javaPackage(configurationData.pomIr)
        val settings = mapOf("package" to javaPackage.asCodePackage())
        TemplatesPlugin::addFileTemplates.execute(
            listOf(
                FileTemplate(AndroidModuleConfigurator.FileTemplateDescriptors.activityMainXml, modulePath, settings),
                FileTemplate(AndroidModuleConfigurator.FileTemplateDescriptors.androidManifestXml, modulePath, settings),
                FileTemplate(AndroidModuleConfigurator.FileTemplateDescriptors.mainActivityKt(javaPackage), modulePath, settings)
            )
        )
    }

    override fun Reader.createAndroidPlugin(module: Module): AndroidGradlePlugin =
        AndroidGradlePlugin.APPLICATION
}