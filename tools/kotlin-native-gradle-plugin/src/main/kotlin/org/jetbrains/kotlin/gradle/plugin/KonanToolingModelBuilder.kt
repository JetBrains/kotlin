/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.tooling.provider.model.ToolingModelBuilder
import org.jetbrains.kotlin.gradle.plugin.model.KonanArtifact
import org.jetbrains.kotlin.gradle.plugin.model.KonanModel
import org.jetbrains.kotlin.konan.KonanVersion
import org.jetbrains.kotlin.konan.util.visibleName

object KonanToolingModelBuilder : ToolingModelBuilder {
    override fun canBuild(modelName: String): Boolean = KonanModel::class.java.name == modelName

    override fun buildAll(modelName: String, project: Project): KonanModel {
        val artifacts = project.konanArtifactsContainer.flatten()
                .toList()
                .map { KonanArtifactImpl("${it.artifact.name}-${it.konanTarget.visibleName}", it.artifact.canonicalPath) }
        return KonanModelImpl(artifacts)
    }
}

private class KonanModelImpl(
        override val artifacts: List<KonanArtifact>
) : KonanModel


private class KonanArtifactImpl(
        override val name: String,
        override val path: String
) : KonanArtifact
