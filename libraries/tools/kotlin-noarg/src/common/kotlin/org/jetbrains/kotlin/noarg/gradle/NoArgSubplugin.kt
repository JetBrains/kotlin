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

package org.jetbrains.kotlin.noarg.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.noarg.gradle.model.builder.NoArgModelBuilder
import javax.inject.Inject

class NoArgGradleSubplugin
@Inject internal constructor(
    private val registry: ToolingModelBuilderRegistry
) : KotlinCompilerPluginSupportPlugin {

    companion object {
        fun getNoArgExtension(project: Project): NoArgExtension {
            return project.extensions.getByType(NoArgExtension::class.java)
        }

        private const val NOARG_ARTIFACT_NAME = "kotlin-noarg-compiler-plugin-embeddable"

        private const val ANNOTATION_ARG_NAME = "annotation"
        private const val PRESET_ARG_NAME = "preset"
        private const val INVOKE_INITIALIZERS_ARG_NAME = "invokeInitializers"
    }

    override fun apply(target: Project) {
        target.extensions.create("noArg", NoArgExtension::class.java)
        registry.register(NoArgModelBuilder())
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project

        return project.provider {
            val noArgExtension = project.extensions.getByType(NoArgExtension::class.java)
            val options = mutableListOf<SubpluginOption>()

            for (anno in noArgExtension.myAnnotations) {
                options += SubpluginOption(ANNOTATION_ARG_NAME, anno)
            }

            for (preset in noArgExtension.myPresets) {
                options += SubpluginOption(PRESET_ARG_NAME, preset)
            }

            if (noArgExtension.invokeInitializers) {
                options += SubpluginOption(INVOKE_INITIALIZERS_ARG_NAME, "true")
            }

            options
        }
    }

    override fun getCompilerPluginId() = "org.jetbrains.kotlin.noarg"
    override fun getPluginArtifact(): SubpluginArtifact =
        JetBrainsSubpluginArtifact(artifactId = NOARG_ARTIFACT_NAME)
}
