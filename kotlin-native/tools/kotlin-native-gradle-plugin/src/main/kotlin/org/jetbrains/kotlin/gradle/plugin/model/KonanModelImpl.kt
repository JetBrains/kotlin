/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle.plugin.model

import org.gradle.api.Project
import org.gradle.tooling.provider.model.ToolingModelBuilder
import org.jetbrains.kotlin.gradle.plugin.konan.konanArtifactsContainer
import org.jetbrains.kotlin.gradle.plugin.konan.konanExtension
import org.jetbrains.kotlin.gradle.plugin.konan.konanHome
import org.jetbrains.kotlin.gradle.plugin.konan.konanVersion
import org.jetbrains.kotlin.konan.CURRENT
import org.jetbrains.kotlin.konan.CompilerVersion
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import java.io.File


object KonanToolingModelBuilder : ToolingModelBuilder {

    override fun canBuild(modelName: String) = KonanModel::class.java.name == modelName

    private fun buildModelKonan(project: Project): KonanModel {
        val artifacts = project.konanArtifactsContainer.flatten().toList().map { it.toModelArtifact() }
        return KonanModelImpl(
            artifacts,
            project.file(project.konanHome),
            project.konanVersion,
            // TODO: Provide defaults for these versions.
            project.konanExtension.languageVersion,
            project.konanExtension.apiVersion
        )
    }

    private val Project.hasKonanPlugin: Boolean
        get() = with(pluginManager) {
            hasPlugin("konan") ||
            hasPlugin("org.jetbrains.kotlin.konan")
        }


    override fun buildAll(modelName: String, project: Project): KonanModel =
        when {
            project.hasKonanPlugin -> buildModelKonan(project)
            else -> throw IllegalStateException("The project '${project.path}' has no Kotlin/Native plugin")
        }
}

internal data class KonanModelImpl(
    override val artifacts: List<KonanModelArtifact>,
    override val konanHome: File,
    override val konanVersion: CompilerVersion,
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
