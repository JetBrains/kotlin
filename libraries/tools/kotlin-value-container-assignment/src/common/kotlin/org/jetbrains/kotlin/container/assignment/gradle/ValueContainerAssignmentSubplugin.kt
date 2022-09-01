/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.container.assignment.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.container.assignment.gradle.model.builder.ValueContainerAssignmentModelBuilder
import org.jetbrains.kotlin.gradle.plugin.*
import javax.inject.Inject

class ValueContainerAssignmentSubplugin
@Inject internal constructor(
    private val registry: ToolingModelBuilderRegistry
) : KotlinCompilerPluginSupportPlugin {

    companion object {
        const val COMPILER_PLUGIN_ARTIFACT_NAME = "kotlin-value-container-assignment"
        const val COMPILER_PLUGIN_ID = "org.jetbrains.kotlin.valueContainerAssignment"
        private const val ANNOTATION_ARG_NAME = "annotation"
    }

    override fun apply(target: Project) {
        target.extensions.create("valueContainerAssignment", ValueContainerAssignmentExtension::class.java)
        registry.register(ValueContainerAssignmentModelBuilder())
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        val extension = project.extensions.findByType(ValueContainerAssignmentExtension::class.java)
            ?: return project.provider { emptyList() }

        return project.provider {
            val options = mutableListOf<SubpluginOption>()
            for (anno in extension.myAnnotations) {
                options += SubpluginOption(ANNOTATION_ARG_NAME, anno)
            }
            options
        }
    }

    override fun getCompilerPluginId(): String = COMPILER_PLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact = JetBrainsSubpluginArtifact(artifactId = COMPILER_PLUGIN_ARTIFACT_NAME)
}