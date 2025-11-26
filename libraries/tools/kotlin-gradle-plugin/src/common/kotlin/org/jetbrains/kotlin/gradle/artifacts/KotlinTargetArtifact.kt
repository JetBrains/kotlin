/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.artifacts

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinGradlePluginExtensionPoint
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

internal fun interface KotlinTargetArtifact {
    suspend fun createArtifact(target: KotlinTarget, apiElements: Configuration, runtimeElements: Configuration?)

    companion object {
        val extensionPoint = KotlinGradlePluginExtensionPoint<KotlinTargetArtifact>()
    }
}

internal fun KotlinTarget.createArtifactsTask(configure: (Jar) -> Unit = {}): TaskProvider<Jar> {
    return project.registerTask<Jar>(artifactsTaskName) { jar ->
        jar.description = "Assembles an archive containing the main classes."
        jar.group = BasePlugin.BUILD_GROUP
        jar.isPreserveFileTimestamps = false
        jar.isReproducibleFileOrder = true

        disambiguationClassifier?.let { classifier ->
            jar.archiveAppendix.set(classifier.toLowerCaseAsciiOnly())
        }

        configure(jar)
    }
}

/**
 * Creates a publish artifact and attaches it to the specified [elementsConfiguration]s.
 *
 * **Gradle 9.0 Compatibility:**
 * This method avoids using the deprecated `archives` configuration. Instead, it:
 * 1. Explicitly declares the [artifactTask] as a dependency of the `assemble` lifecycle task.
 * 2. Registers the artifact directly on the provided [elementsConfiguration]s.
 *
 * @param artifactTask The provider of the task that produces the artifact (e.g. a Jar task).
 * @param artifactType The standard Gradle artifact type (e.g. "jar").
 * @param elementsConfiguration Vararg of configurations (usually `apiElements` or `runtimeElements`) where this artifact should be exposed.
 * @return The created [PublishArtifact] handle.
 */
internal fun KotlinTarget.createPublishArtifact(
    artifactTask: TaskProvider<*>,
    artifactType: String,
    vararg elementsConfiguration: Configuration?,
): PublishArtifact {
    // 1. Lifecycle: Ensure the artifact is built when 'assemble' runs.
    project.tasks.named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME).configure { assembleTask ->
        assembleTask.dependsOn(artifactTask)
    }

    val configurations = elementsConfiguration.filterNotNull()

    // Safety check: We need at least one configuration to register the artifact against
    // to generate a valid PublishArtifact object easily, or we use a fallback strategy.
    val primaryConfig = configurations.firstOrNull()
        ?: project.configurations.detachedConfiguration() // Fallback if no config provided, just to create the object

    // 2. Create the artifact handle by registering it on the primary configuration.
    // Using project.artifacts.add returns the PublishArtifact object we need to return.
    val artifact = project.artifacts.add(primaryConfig.name, artifactTask) { artifact ->
        artifact.builtBy(artifactTask)
        artifact.type = artifactType
    }

    // 3. Configure attributes and add to any additional configurations.
    configurations.forEach { configuration ->
        // If it's the primary config, the artifact is already added (step 2), but we strictly ensure
        // attributes are set. If it's a secondary config, we add the artifact reference.
        if (configuration != primaryConfig) {
            configuration.outgoing.artifacts.add(artifact)
        }

        configuration.outgoing.attributes.attribute(
            ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
            artifactType
        )
    }

    return artifact
}
