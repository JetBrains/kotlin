package org.jetbrains.kotlin.gradle.plugin.model

import org.gradle.api.Project
import org.gradle.tooling.provider.model.ToolingModelBuilder
import org.jetbrains.kotlin.gradle.plugin.konanArtifactsContainer
import org.jetbrains.kotlin.gradle.plugin.konanExtension
import org.jetbrains.kotlin.gradle.plugin.konanHome
import org.jetbrains.kotlin.gradle.plugin.tasks.KonanBuildingTask
import org.jetbrains.kotlin.gradle.plugin.tasks.KonanCompileTask
import org.jetbrains.kotlin.gradle.plugin.tasks.Produce
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File


object KonanToolingModelBuilder : ToolingModelBuilder {

    override fun canBuild(modelName: String) = KonanModel::class.java.name == modelName

    override fun buildAll(modelName: String, project: Project): KonanModel {
        val artifacts = project.konanArtifactsContainer.flatten().toList().map { it.toModelArtifact() }
        return KonanModelImpl(
                artifacts,
                project.konanHome,
                // TODO: Provide a better support for the default version
                project.konanExtension.languageVersion ?: "1.2"
        )
    }
}

internal data class KonanModelImpl(
        override val artifacts: List<KonanModelArtifact>,
        override val konanHome: String,
        override val konanVersion: String
) : KonanModel

internal data class KonanModelArtifactImpl(
        override val name: String,
        override val file: File,
        override val type: Produce,
        override val targetPlatform: KonanTarget,
        override val buildTaskName: String,
        override val srcDirs: List<File>,
        override val srcFiles: List<File>,
        override val libraries: List<File>
) : KonanModelArtifact
