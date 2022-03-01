/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm

import org.gradle.api.Project
import org.gradle.api.artifacts.component.BuildIdentifier
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.BasePluginConvention
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.internal.build.BuildState
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.utils.isGradleVersionAtLeast
import org.jetbrains.kotlin.project.model.KotlinModule
import org.jetbrains.kotlin.project.model.LocalModuleIdentifier

val KotlinProjectExtension.targets: Iterable<KotlinTarget>
    get() = when (this) {
        is KotlinSingleTargetExtension -> listOf(this.target)
        is KotlinMultiplatformExtension -> targets
        else -> error("Unexpected 'kotlin' extension $this")
    }

fun KotlinModule.representsProject(project: Project): Boolean =
    moduleIdentifier.let { it is LocalModuleIdentifier && it.buildId == project.currentBuildId().name && it.projectId == project.path }

// FIXME internal API?
fun Project.currentBuildId(): BuildIdentifier =
    (project as ProjectInternal).services.get(BuildState::class.java).buildIdentifier


/**
 * The base name to use for archive files.
 */
val Project.archivesName get() = if (isGradleVersionAtLeast(7, 1)) {
        extensions.getByType(BasePluginExtension::class.java).archivesName.orNull
    } else {
        convention.findPlugin(BasePluginConvention::class.java)?.archivesBaseName
    }

/**
 * Returns the directory to generate TAR and ZIP archives into.
 *
 * @return The directory. Never returns null.
 */
val Project.distsDirectory get() = if (isGradleVersionAtLeast(7, 1)) {
        extensions.getByType(BasePluginExtension::class.java).distsDirectory
    } else {
        convention.getPlugin(BasePluginConvention::class.java).distsDirectory
    }