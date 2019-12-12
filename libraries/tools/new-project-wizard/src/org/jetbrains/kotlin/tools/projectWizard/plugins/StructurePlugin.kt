package org.jetbrains.kotlin.tools.projectWizard.plugins

import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.Plugin
import org.jetbrains.kotlin.tools.projectWizard.core.TaskRunningContext
import org.jetbrains.kotlin.tools.projectWizard.core.entity.reference
import org.jetbrains.kotlin.tools.projectWizard.core.pathParser
import org.jetbrains.kotlin.tools.projectWizard.core.service.FileSystemWizardService
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.PomIR
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version
import java.nio.file.Paths

class StructurePlugin(context: Context) : Plugin(context) {
    val projectPath by valueSetting("Root path", GenerationPhase.PROJECT_GENERATION, pathParser) {
        defaultValue = Paths.get(".")
    }
    val name by stringSetting("Name", GenerationPhase.PROJECT_GENERATION)


    val groupId by stringSetting("Artifact ID", GenerationPhase.PROJECT_GENERATION) {
        shouldNotBeBlank()
    }
    val artifactId by stringSetting("Group ID", GenerationPhase.PROJECT_GENERATION) {
        shouldNotBeBlank()
    }
    val version by stringSetting("Version", GenerationPhase.PROJECT_GENERATION) {
        shouldNotBeBlank()
        defaultValue = "1.0-SNAPSHOT"
    }

    val createProjectDir by pipelineTask(GenerationPhase.PROJECT_GENERATION) {
        withAction {
            service<FileSystemWizardService>()!!.createDirectory(StructurePlugin::projectPath.reference.settingValue)
        }
    }
}

val TaskRunningContext.projectPath
    get() = StructurePlugin::projectPath.reference.settingValue

fun TaskRunningContext.pomIR() = PomIR(
    artifactId = StructurePlugin::artifactId.reference.settingValue,
    groupId = StructurePlugin::groupId.reference.settingValue,
    version = Version.fromString(StructurePlugin::version.reference.settingValue)
)
