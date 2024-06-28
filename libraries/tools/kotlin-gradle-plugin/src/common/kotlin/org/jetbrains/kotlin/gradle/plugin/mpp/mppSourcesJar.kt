/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.Future
import org.jetbrains.kotlin.gradle.utils.future
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import java.io.File


internal fun sourcesJarTask(compilation: KotlinCompilation<*>, componentName: String, artifactNameAppendix: String): TaskProvider<Jar> =
    sourcesJarTask(
        project = compilation.target.project,
        sourceSets = compilation.target.project.future {
            compilation.internal.awaitAllKotlinSourceSets().associate { it.name to it.kotlin }
        },
        componentName = componentName,
        artifactNameAppendix = artifactNameAppendix
    )

private fun sourcesJarTask(
    project: Project,
    sourceSets: Future<Map<String, Iterable<File>>>,
    componentName: String,
    artifactNameAppendix: String,
): TaskProvider<Jar> =
    sourcesJarTaskNamed(
        taskName = lowerCamelCaseName(componentName, "sourcesJar"),
        componentName = componentName,
        project = project,
        sourceSets = sourceSets,
        artifactNameAppendix = artifactNameAppendix
    )

internal fun sourcesJarTaskNamed(
    taskName: String,
    componentName: String,
    project: Project,
    sourceSets: Future<Map<String, Iterable<File>>>,
    artifactNameAppendix: String,
    componentTypeName: String = "target",
): TaskProvider<Jar> {
    project.locateTask<Jar>(taskName)?.let {
        return it
    }

    val result = project.registerTask<Jar>(taskName) { sourcesJar ->
        sourcesJar.archiveAppendix.set(artifactNameAppendix)
        sourcesJar.archiveClassifier.set("sources")
        sourcesJar.isPreserveFileTimestamps = false
        sourcesJar.isReproducibleFileOrder = true
        sourcesJar.group = BasePlugin.BUILD_GROUP
        sourcesJar.description = "Assembles a jar archive containing the sources of $componentTypeName '$componentName'."
        project.launch {
            sourcesJar.includeSources(sourceSets.await())
        }
    }

    return result
}

internal fun Jar.includeSources(compilation: KotlinCompilation<*>) {
    compilation.internal.allKotlinSourceSets.forAll { sourceSet ->
        includeSources(sourceSet.name, sourceSet.kotlin)
    }
}

internal fun Jar.includeSources(sourceSets: Map<String, Iterable<File>>) {
    sourceSets.forEach { (name, sources) -> includeSources(name, sources) }
}

internal fun Jar.includeSources(name: String, sources: Iterable<File>) {
    from(sources) { spec ->
        spec.into(name)
        // Duplicates are coming from `SourceSets` that `sourceSet` depends on.
        // Such dependency was added by Kotlin compilation.
        // TODO: rethink approach for adding dependent `SourceSets` to Kotlin compilation `SourceSet`
        spec.duplicatesStrategy = DuplicatesStrategy.WARN
    }
}