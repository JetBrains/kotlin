/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.*

class LombokSubplugin : KotlinCompilerPluginSupportPlugin {

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean =
        (kotlinCompilation.platformType == KotlinPlatformType.jvm || kotlinCompilation.platformType == KotlinPlatformType.androidJvm)

    override fun apply(target: Project) {
        target.extensions.create("kotlinLombok", LombokExtension::class.java)
    }

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project

        return project.provider {
            val extension = project.extensions.getByType(LombokExtension::class.java)
            val options = mutableListOf<SubpluginOption>()

            extension.configurationFile?.let { configFile ->
                options += SubpluginOption("config", configFile.absolutePath)
            }
            options
        }
    }

    override fun getCompilerPluginId(): String = "org.jetbrains.kotlin.lombok"

    override fun getPluginArtifact(): SubpluginArtifact = JetBrainsSubpluginArtifact(artifactId = "kotlin-lombok")
}
