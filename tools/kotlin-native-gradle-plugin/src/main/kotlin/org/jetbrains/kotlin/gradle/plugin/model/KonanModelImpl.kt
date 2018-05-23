package org.jetbrains.kotlin.gradle.plugin.model

import org.gradle.api.Project
import org.gradle.tooling.provider.model.ToolingModelBuilder
import org.jetbrains.kotlin.gradle.plugin.konanArtifactsContainer
import org.jetbrains.kotlin.gradle.plugin.konanExtension
import org.jetbrains.kotlin.gradle.plugin.konanHome
import org.jetbrains.kotlin.konan.KonanVersion
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import java.io.File


object KonanToolingModelBuilder : ToolingModelBuilder {

    override fun canBuild(modelName: String) = KonanModel::class.java.name == modelName

    override fun buildAll(modelName: String, project: Project): KonanModel {
        val artifacts = project.konanArtifactsContainer.flatten().toList().map { it.toModelArtifact() }
        return KonanModelImpl(
                artifacts,
                project.file(project.konanHome),
                KonanVersion.CURRENT,
                // TODO: Provide defaults for these versions.
                project.konanExtension.languageVersion,
                project.konanExtension.apiVersion
        )
    }
}

internal data class KonanModelImpl(
        override val artifacts: List<KonanModelArtifact>,
        override val konanHome: File,
        override val konanVersion: KonanVersion,
        override val languageVersion: String?,
        override val apiVersion: String?
) : KonanModel

internal data class KonanModelArtifactImpl(
        override val name: String,
        override val file: File,
        override val type: CompilerOutputKind,
        override val targetPlatform: String,
        override val buildTaskName: String,
        override val srcDirs: List<File>,
        override val srcFiles: List<File>,
        override val libraries: List<File>,
        override val searchPaths: List<File>
) : KonanModelArtifact
