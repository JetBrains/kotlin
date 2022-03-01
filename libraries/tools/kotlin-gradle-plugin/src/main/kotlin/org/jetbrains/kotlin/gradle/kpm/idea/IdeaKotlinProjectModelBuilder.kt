package org.jetbrains.kotlin.gradle.kpm.idea

import org.gradle.api.Project
import org.gradle.tooling.provider.model.ToolingModelBuilder
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.gradle.dsl.kpmExtension
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.kpm.KpmExtension
import java.io.File

class IdeaKotlinProjectModelBuilder : ToolingModelBuilder {
    override fun canBuild(modelName: String): Boolean = modelName == IdeaKotlinProjectModel::class.java.name
    override fun buildAll(modelName: String, project: Project): IdeaKotlinProjectModel {
        return project.kpmExtension.toIdeaKotlinProjectModel()
    }
}

internal fun KpmExtension.toIdeaKotlinProjectModel(): IdeaKotlinProjectModel {
    return IdeaKotlinProjectModelImpl(
        gradlePluginVersion = project.getKotlinPluginVersion(),
        coreLibrariesVersion = coreLibrariesVersion,
        explicitApiModeCliOption = explicitApi?.cliOption,
        kotlinNativeHome = File(project.konanHome).absoluteFile,
        modules = modules.map { module -> module.toIdeaKotlinModule() }
    )
}
