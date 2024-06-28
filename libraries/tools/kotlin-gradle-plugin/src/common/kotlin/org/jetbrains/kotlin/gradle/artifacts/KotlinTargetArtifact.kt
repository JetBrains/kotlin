/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.artifacts

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency.ARCHIVES_CONFIGURATION
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.jetbrains.kotlin.gradle.plugin.KotlinGradlePluginExtensionPoint
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.internal.artifactTypeAttribute
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.setAttribute
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

internal fun KotlinTarget.createPublishArtifact(
    artifactTask: TaskProvider<*>,
    artifactType: String,
    vararg elementsConfiguration: Configuration?,
): PublishArtifact {
    val artifact = project.artifacts.add(ARCHIVES_CONFIGURATION, artifactTask) { artifact ->
        artifact.builtBy(artifactTask)
        artifact.type = artifactType
    }

    elementsConfiguration.filterNotNull().forEach { configuration ->
        configuration.outgoing.artifacts.add(artifact)
        configuration.outgoing.attributes.setAttribute(project.artifactTypeAttribute, artifactType)
    }

    return artifact
}
