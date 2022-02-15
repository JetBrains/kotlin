package org.jetbrains.kotlin.gradle.kpm.idea

import org.gradle.api.Project
import org.gradle.tooling.provider.model.ToolingModelBuilder
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.gradle.dsl.pm20Extension
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinPm20ProjectExtension
import java.io.File

class IdeaKotlinProjectModelBuilder : ToolingModelBuilder {
    override fun canBuild(modelName: String): Boolean = modelName == IdeaKotlinProjectModel::class.java.name
    override fun buildAll(modelName: String, project: Project): IdeaKotlinProjectModel {
        return project.pm20Extension.toIdeaKotlinProjectModel()
    }
}

internal fun KotlinPm20ProjectExtension.toIdeaKotlinProjectModel(): IdeaKotlinProjectModel {
    return IdeaKotlinProjectModelImpl(
        gradlePluginVersion = project.getKotlinPluginVersion(),
        coreLibrariesVersion = coreLibrariesVersion,
        explicitApiModeCliOption = explicitApi?.cliOption,
        kotlinNativeHome = File(project.konanHome).absoluteFile,
        modules = modules.map { module -> module.toIdeaKotlinModule() }
    )
}
