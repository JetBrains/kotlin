/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.coverage

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class CoverageCompilerSubplugin : KotlinCompilerPluginSupportPlugin {
    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        return kotlinCompilation.target.project.provider { emptyList() }
    }

    override fun apply(target: Project) {
        super.apply(target)
    }

    override fun getCompilerPluginId(): String = "org.jetbrains.kotlin.coverage"

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(GROUP_NAME, ARTIFACT_NAME)

    companion object {
        const val GROUP_NAME = "org.jetbrains.kotlin"
        const val ARTIFACT_NAME = "coverage-compiler-plugin-embeddable"
    }
}