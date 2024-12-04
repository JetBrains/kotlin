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

package org.jetbrains.kotlin.samWithReceiver.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.noarg.gradle.model.builder.SamWithReceiverModelBuilder
import javax.inject.Inject

class SamWithReceiverGradleSubplugin
@Inject internal constructor(
    private val registry: ToolingModelBuilderRegistry
) : KotlinCompilerPluginSupportPlugin {

    override fun apply(target: Project) {
        target.extensions.create("samWithReceiver", SamWithReceiverExtension::class.java)
        registry.register(SamWithReceiverModelBuilder())
    }

    companion object {
        const val SAM_WITH_RECEIVER_ARTIFACT_NAME = "kotlin-sam-with-receiver-compiler-plugin-embeddable"

        private const val ANNOTATION_ARG_NAME = "annotation"
        private const val PRESET_ARG_NAME = "preset"
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project

        val samWithReceiverExtension =
            project.extensions.findByType(SamWithReceiverExtension::class.java) ?: return project.provider { emptyList<SubpluginOption>() }

        return project.provider {
            val options = mutableListOf<SubpluginOption>()

            for (anno in samWithReceiverExtension.myAnnotations) {
                options += SubpluginOption(ANNOTATION_ARG_NAME, anno)
            }

            for (preset in samWithReceiverExtension.myPresets) {
                options += SubpluginOption(PRESET_ARG_NAME, preset)
            }

            options
        }
    }

    override fun getCompilerPluginId() = "org.jetbrains.kotlin.samWithReceiver"
    override fun getPluginArtifact(): SubpluginArtifact =
        JetBrainsSubpluginArtifact(artifactId = SAM_WITH_RECEIVER_ARTIFACT_NAME)
}
