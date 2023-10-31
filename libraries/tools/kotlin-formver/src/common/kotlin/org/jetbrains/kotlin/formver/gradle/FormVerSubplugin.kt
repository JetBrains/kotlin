/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.formver.gradle.model.builder.FormVerModelBuilder
import org.jetbrains.kotlin.formver.FormalVerificationPluginNames
import org.jetbrains.kotlin.gradle.plugin.*
import javax.inject.Inject

class FormVerGradleSubplugin
@Inject internal constructor(
    private val registry: ToolingModelBuilderRegistry,
) : KotlinCompilerPluginSupportPlugin {
    companion object {
        private const val FORMVER_ARTIFACT_NAME = "kotlin-formver-compiler-plugin-embeddable"
    }

    override fun apply(target: Project) {
        target.extensions.create("formver", FormVerExtension::class.java)
        registry.register(FormVerModelBuilder())
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project

        val formVerExtension = project.extensions.getByType(FormVerExtension::class.java)

        return project.provider {
            val options = mutableListOf<SubpluginOption>()

            formVerExtension.myLogLevel?.let {
                options += SubpluginOption(FormalVerificationPluginNames.LOG_LEVEL_OPTION_NAME, it)
            }

            formVerExtension.myErrorStyle?.let {
                options += SubpluginOption(FormalVerificationPluginNames.ERROR_STYLE_NAME, it)
            }

            formVerExtension.myUnsupportedFeatureBehaviour?.let {
                options += SubpluginOption(FormalVerificationPluginNames.UNSUPPORTED_FEATURE_BEHAVIOUR_OPTION_NAME, it)
            }

            formVerExtension.myConversionTargetsSelection?.let {
                options += SubpluginOption(FormalVerificationPluginNames.CONVERSION_TARGETS_SELECTION_OPTION_NAME, it)
            }

            formVerExtension.myVerificationTargetsSelection?.let {
                options += SubpluginOption(FormalVerificationPluginNames.VERIFICATION_TARGETS_SELECTION_OPTION_NAME, it)
            }

            options
        }
    }

    override fun getCompilerPluginId(): String = "org.jetbrains.kotlin.formver"

    override fun getPluginArtifact(): SubpluginArtifact =
        JetBrainsSubpluginArtifact(artifactId = FORMVER_ARTIFACT_NAME)
}