/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.tasks.artifact

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeArtifactDSL.ExperimentalArtifactDsl
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnosticOncePerProject
import org.jetbrains.kotlin.gradle.utils.castIsolatedKotlinPluginClassLoaderAware
import javax.inject.Inject

private const val KOTLIN_ARTIFACTS_EXTENSION_NAME = "kotlinArtifacts"

internal val KotlinArtifactsExtensionSetupAction = KotlinProjectSetupAction {
    val kotlinArtifactsExt = objects.newInstance(KotlinArtifactsExtensionImpl::class.java, this)
    extensions.add(KOTLIN_ARTIFACTS_EXTENSION_NAME, kotlinArtifactsExt)
    kotlinArtifactsExt.artifacts.all { it.registerAssembleTask(this) }
}

val Project.kotlinArtifactsExtension: KotlinArtifactsExtension
    get() = extensions.getByName(KOTLIN_ARTIFACTS_EXTENSION_NAME).castIsolatedKotlinPluginClassLoaderAware()

@OptIn(ExperimentalArtifactDsl::class)
abstract class KotlinNativeArtifactDSLImpl @Inject constructor(private val project: Project) : KotlinNativeArtifactDSL {
    companion object {
        private val UNSAFE_NAME_SYMBOLS = """\W""".toRegex()
    }

    override fun Library(name: String, configure: Action<KotlinNativeLibraryConfig>) {
        addKotlinArtifact<KotlinNativeLibraryConfigImpl>(name, configure)
    }

    override fun Library(configure: Action<KotlinNativeLibraryConfig>) {
        addKotlinArtifact<KotlinNativeLibraryConfigImpl>(configure)
    }

    override fun Framework(name: String, configure: Action<KotlinNativeFrameworkConfig>) {
        addKotlinArtifact<KotlinNativeFrameworkConfigImpl>(name, configure)
    }

    override fun Framework(configure: Action<KotlinNativeFrameworkConfig>) {
        addKotlinArtifact<KotlinNativeFrameworkConfigImpl>(configure)
    }

    override fun FatFramework(name: String, configure: Action<KotlinNativeFatFrameworkConfig>) {
        addKotlinArtifact<KotlinNativeFatFrameworkConfigImpl>(name, configure)
    }

    override fun FatFramework(configure: Action<KotlinNativeFatFrameworkConfig>) {
        addKotlinArtifact<KotlinNativeFatFrameworkConfigImpl>(configure)
    }

    override fun XCFramework(name: String, configure: Action<KotlinNativeXCFrameworkConfig>) {
        addKotlinArtifact<KotlinNativeXCFrameworkConfigImpl>(name, configure)
    }

    override fun XCFramework(configure: Action<KotlinNativeXCFrameworkConfig>) {
        addKotlinArtifact<KotlinNativeXCFrameworkConfigImpl>(configure)
    }

    private inline fun <reified T : KotlinArtifactConfig> addKotlinArtifact(configure: Action<in T>) {
        addKotlinArtifact(project.name.replace(UNSAFE_NAME_SYMBOLS, "_"), configure)
    }

    private inline fun <reified T : KotlinArtifactConfig> addKotlinArtifact(name: String, configure: Action<in T>) {
        if (!project.kotlinPropertiesProvider.suppressExperimentalArtifactsDslWarning) {
            project.reportDiagnosticOncePerProject(KotlinToolingDiagnostics.ExperimentalArtifactsDslUsed())
        }

        //create via newInstance for extensibility
        val config: T = project.objects.newInstance(T::class.java, name)
        project.kotlinArtifactsExtension.artifactConfigs.add(config)

        //current project is added by default
        config.addModule(project)

        //apply user configuration
        configure.execute(config)
        //create immutable artifact object
        val artifact = config.createArtifact(config as ExtensionAware)

        val isAdded = project.kotlinArtifactsExtension.artifacts.add(artifact)
        if (!isAdded) {
            error("Kotlin artifact '${artifact.name}' is already exists! Change the name, please!")
        }
    }
}