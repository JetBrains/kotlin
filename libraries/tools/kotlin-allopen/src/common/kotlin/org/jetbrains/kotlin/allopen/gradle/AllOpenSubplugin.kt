/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.allopen.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.allopen.gradle.model.builder.AllOpenModelBuilder
import org.jetbrains.kotlin.gradle.plugin.*
import javax.inject.Inject

class AllOpenGradleSubplugin
@Inject internal constructor(
    private val registry: ToolingModelBuilderRegistry
) : KotlinCompilerPluginSupportPlugin {

    companion object {
        fun getAllOpenExtension(project: Project): AllOpenExtension {
            return project.extensions.getByType(AllOpenExtension::class.java)
        }

        private const val ALLOPEN_ARTIFACT_NAME = "kotlin-allopen-compiler-plugin-embeddable"

        private const val ANNOTATION_ARG_NAME = "annotation"
        private const val PRESET_ARG_NAME = "preset"
    }

    override fun apply(target: Project) {
        target.extensions.create("allOpen", AllOpenExtension::class.java)
        registry.register(AllOpenModelBuilder())
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project

        val allOpenExtension = project.extensions.getByType(AllOpenExtension::class.java)

        return project.provider {
            val options = mutableListOf<SubpluginOption>()

            for (anno in allOpenExtension.myAnnotations) {
                options += SubpluginOption(ANNOTATION_ARG_NAME, anno)
            }

            for (preset in allOpenExtension.myPresets) {
                options += SubpluginOption(PRESET_ARG_NAME, preset)
            }

            options
        }
    }

    override fun getCompilerPluginId() = "org.jetbrains.kotlin.allopen"
    override fun getPluginArtifact(): SubpluginArtifact =
        JetBrainsSubpluginArtifact(artifactId = ALLOPEN_ARTIFACT_NAME)
}
