package org.jetbrains.kotlin.tools.projectWizard.plugins



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
    val projectPath by pathSetting("Location", GenerationPhase.FIRST_STEP) {
        defaultValue = value(Paths.get("."))
    }
    val name by stringSetting("Name", GenerationPhase.FIRST_STEP) {
        shouldNotBeBlank()
        validate(StringValidators.shouldBeValidIdentifier("Name", Module.ALLOWED_SPECIAL_CHARS_IN_MODULE_NAMES))
    }

    val groupId by stringSetting("Group ID", GenerationPhase.FIRST_STEP) {
        isSavable = true
        shouldNotBeBlank()
        validate(StringValidators.shouldBeValidIdentifier("Group ID", setOf('.', '_')))
    }
    val artifactId by stringSetting("Artifact ID", GenerationPhase.FIRST_STEP) {
        shouldNotBeBlank()
        validate(StringValidators.shouldBeValidIdentifier("Artifact ID", setOf('_')))
    }
    val version by stringSetting("Version", GenerationPhase.FIRST_STEP) {
        shouldNotBeBlank()
        validate(StringValidators.shouldBeValidIdentifier("Version", setOf('_', '-', '.')))
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
