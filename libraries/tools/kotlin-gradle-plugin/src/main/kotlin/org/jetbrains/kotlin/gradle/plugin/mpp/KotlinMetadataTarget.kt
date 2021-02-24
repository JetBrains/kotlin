/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurablePublishArtifact
import org.gradle.api.artifacts.Dependency
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.targets.metadata.KotlinMetadataTargetConfigurator
import org.jetbrains.kotlin.gradle.targets.metadata.isCompatibilityMetadataVariantEnabled
import org.jetbrains.kotlin.gradle.targets.metadata.isKotlinGranularMetadataEnabled
import javax.inject.Inject

open class KotlinMetadataTarget @Inject constructor(project: Project) :
    KotlinOnlyTarget<AbstractKotlinCompilation<*>>(project, KotlinPlatformType.common) {

    override val artifactsTaskName: String
        // The IDE import looks at this task name to determine the artifact and register the path to the artifact;
        // in HMPP, since the project resolves to the all-metadata JAR, the IDE import needs to work with that JAR, too
        get() = if (project.isKotlinGranularMetadataEnabled) KotlinMetadataTargetConfigurator.ALL_METADATA_JAR_NAME else super.artifactsTaskName

    internal val legacyArtifactsTaskName: String
        get() = super.artifactsTaskName

    override val kotlinComponents: Set<KotlinTargetComponent> by lazy {
        /*
        Metadata Target does not have a KotlinTargetComponent on it's own.
        Responsibility is shifted to the root KotlinSoftwareComponent
        */
        emptySet<KotlinTargetComponent>()
    }
}

