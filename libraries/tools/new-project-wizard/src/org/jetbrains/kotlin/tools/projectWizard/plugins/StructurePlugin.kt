package org.jetbrains.kotlin.tools.projectWizard.plugins


import org.jetbrains.kotlin.tools.projectWizard.KotlinNewProjectWizardBundle
import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.entity.StringValidators
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.reference
import org.jetbrains.kotlin.tools.projectWizard.core.service.FileSystemWizardService
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.PomIR
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version
import java.nio.file.Paths

class StructurePlugin(context: Context) : Plugin(context) {
    val projectPath by pathSetting(
        KotlinNewProjectWizardBundle.message("plugin.structure.setting.location"),
        GenerationPhase.FIRST_STEP
    ) {
        defaultValue = value(Paths.get("."))
    }
    val name by stringSetting(
        KotlinNewProjectWizardBundle.message("plugin.structure.setting.name"),
        GenerationPhase.FIRST_STEP
    ) {
        shouldNotBeBlank()
        validate(StringValidators.shouldBeValidIdentifier(title, Module.ALLOWED_SPECIAL_CHARS_IN_MODULE_NAMES))
    }

    val groupId by stringSetting(
        KotlinNewProjectWizardBundle.message("plugin.structure.setting.group.id"),
        GenerationPhase.FIRST_STEP
    ) {
        isSavable = true
        shouldNotBeBlank()
        validate(StringValidators.shouldBeValidIdentifier(title, setOf('.', '_')))
    }
    val artifactId by stringSetting(
        KotlinNewProjectWizardBundle.message("plugin.structure.setting.artifact.id"),
        GenerationPhase.FIRST_STEP
    ) {
        shouldNotBeBlank()
        validate(StringValidators.shouldBeValidIdentifier(title, setOf('_')))
    }
    val version by stringSetting(
        KotlinNewProjectWizardBundle.message("plugin.structure.setting.version"),
        GenerationPhase.FIRST_STEP
    ) {
        shouldNotBeBlank()
        validate(StringValidators.shouldBeValidIdentifier(title, setOf('_', '-', '.')))
        defaultValue = value("1.0-SNAPSHOT")
    }

    val createProjectDir by pipelineTask(GenerationPhase.PROJECT_GENERATION) {
        withAction {
            service<FileSystemWizardService>().createDirectory(StructurePlugin::projectPath.reference.settingValue)
        }
    }
}

val Reader.projectPath
    get() = StructurePlugin::projectPath.reference.settingValue

val Reader.projectName
    get() = StructurePlugin::name.reference.settingValue


fun Writer.pomIR() = PomIR(
    artifactId = StructurePlugin::artifactId.reference.settingValue,
    groupId = StructurePlugin::groupId.reference.settingValue,
    version = Version.fromString(StructurePlugin::version.reference.settingValue)
)
