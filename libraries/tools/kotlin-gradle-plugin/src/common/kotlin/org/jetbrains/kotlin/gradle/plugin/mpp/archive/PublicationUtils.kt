/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.archive

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.DefaultKotlinUsageContext
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsageContext
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

private fun Configuration.addOutgoingKarArtifactTo(karPackTask: TaskProvider<PackKotlinArchiveTask>) {
    outgoing.artifact(karPackTask) {
        it.extension = KarLayout.KAR_XZ_PACKED_EXTENSION
    }
    outgoing.attributes.attribute(KarLayout.Attributes.compressionMethod, KarLayout.Attributes.CompressionMethod.XZ)
}

internal fun Project.defaultKotlinUsageContextMaybeReplacedWithKar(
    isStoredInKotlinArchive: Provider<Boolean>?,
    compilation: KotlinCompilation<*>,
    mavenScope: KotlinUsageContext.MavenScope?,
    dependencyConfigurationName: String,
    includeIntoProjectStructureMetadata: Boolean = true,
    publishOnlyIf: DefaultKotlinUsageContext.PublishOnlyIf = DefaultKotlinUsageContext.PublishOnlyIf { true },
): DefaultKotlinUsageContext {
    val overrideConfigurationArtifacts: Provider<Set<PublishArtifact>>? = isStoredInKotlinArchive?.map { if (it) emptySet<PublishArtifact>() else null }
    val karTaskProvider = karPackTask
    return DefaultKotlinUsageContext(
        compilation = compilation,
        mavenScope = mavenScope,
        dependencyConfigurationName = dependencyConfigurationName,
        includeIntoProjectStructureMetadata = includeIntoProjectStructureMetadata,
        publishOnlyIf = publishOnlyIf,
        overrideConfigurationArtifacts = overrideConfigurationArtifacts,
        configurePublishedConfiguration = {
            if (isStoredInKotlinArchive?.orNull == true) {
                addOutgoingKarArtifactTo(karTaskProvider)
            }
        }
    )
}
