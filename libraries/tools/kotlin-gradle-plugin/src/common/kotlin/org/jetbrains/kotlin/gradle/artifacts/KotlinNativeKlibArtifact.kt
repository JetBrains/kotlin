/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.artifacts

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.internal.plugins.DefaultArtifactPublicationSet
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationInfo
import org.jetbrains.kotlin.gradle.plugin.KotlinNativeTargetConfigurator.NativeArtifactFormat
import org.jetbrains.kotlin.gradle.plugin.internal.artifactTypeAttribute
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.enabledOnCurrentHost
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.setAttribute
import java.io.File

internal val KotlinNativeKlibArtifact = KotlinTargetArtifact { target, apiElements, _ ->
    if (target !is KotlinNativeTarget) return@KotlinTargetArtifact
    /* Just registering a dummy placeholder that other tasks can use as umbrella */
    val artifactsTask = target.project.registerTask<DefaultTask>(target.artifactsTaskName) {
        it.group = BasePlugin.BUILD_GROUP
        it.description = "Assembles outputs for target '${target.name}'."
    }

    apiElements.outgoing.attributes.setAttribute(target.project.artifactTypeAttribute, NativeArtifactFormat.KLIB)

    target.compilations.getByName(MAIN_COMPILATION_NAME).let { mainCompilation ->
        artifactsTask.dependsOn(mainCompilation.compileTaskProvider)
        createRegularKlibArtifact(mainCompilation)
    }
}

internal fun createRegularKlibArtifact(compilation: AbstractKotlinNativeCompilation) = createKlibArtifact(
    compilation = compilation,
    artifactFile = compilation.compileTaskProvider.map { it.outputFile.get() },
    classifier = null,
    producingTask = compilation.compileTaskProvider
)

internal fun createKlibArtifact(
    compilation: AbstractKotlinNativeCompilation,
    artifactFile: Provider<File>,
    classifier: String?,
    producingTask: TaskProvider<*>,
) {
    if (!compilation.konanTarget.enabledOnCurrentHost) {
        return
    }

    val apiElementsName = compilation.target.apiElementsConfigurationName
    with(compilation.project.configurations.getByName(apiElementsName)) {
        val klibArtifact = compilation.project.artifacts.add(name, artifactFile) { artifact ->
            artifact.name = compilation.compilationName
            artifact.extension = "klib"
            artifact.type = "klib"
            artifact.classifier = classifier
            artifact.builtBy(producingTask)
        }
        compilation.project.extensions.getByType(DefaultArtifactPublicationSet::class.java).addCandidate(klibArtifact)
        artifacts.add(klibArtifact)
        attributes.setAttribute(compilation.project.artifactTypeAttribute, NativeArtifactFormat.KLIB)
    }
}

internal fun Project.klibOutputDirectory(
    compilation: KotlinCompilationInfo,
): DirectoryProperty {
    val targetSubDirectory = compilation.targetDisambiguationClassifier?.let { "$it/" }.orEmpty()
    return project.objects.directoryProperty().value(
        layout.buildDirectory.dir("classes/kotlin/$targetSubDirectory${compilation.compilationName}")
    )
}
