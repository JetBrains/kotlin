/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(InternalIdeApi::class)

package org.jetbrains.kotlin.gradle.plugin.ide

import org.gradle.api.Project
import org.gradle.api.artifacts.component.BuildIdentifier
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleFragment
import org.jetbrains.kotlin.gradle.kpm.currentBuildId
import java.io.File

/**
 * Entity sent to the IDE for importing dependencies of [KotlinGradleFragment]s
 * @see IdeLocalSourceFragmentDependency
 * @see IdeMavenBinaryFragmentDependency
 */
@InternalIdeApi
sealed class IdeFragmentDependency

/**
 * "Project to Project" / "Source" Dependency on any Gradle project's fragment
 */
@InternalIdeApi
data class IdeLocalSourceFragmentDependency(
    val buildId: BuildIdentifier,
    val projectPath: String,
    val projectName: String,
    val kotlinModuleName: String,
    val kotlinFragmentName: String
) : IdeFragmentDependency() {
    constructor(
        project: Project,
        kotlinModuleName: String,
        kotlinFragmentName: String
    ) : this(
        buildId = project.currentBuildId(),
        projectPath = project.path,
        projectName = project.name,
        kotlinModuleName = kotlinModuleName,
        kotlinFragmentName = kotlinFragmentName
    )
}

/**
 * Binary dependency provided by a (maven) repository
 * Can be identified by maven coordinates (group, module, version).
 * One artifact downloaded by such repository might get transformed into several dependencies
 * on certain [kotlinFragmentName].
 */
@InternalIdeApi
data class IdeMavenBinaryFragmentDependency(
    val mavenGroup: String,
    val mavenModule: String,
    val version: String,
    /**
     * `null` when binary dependency was not resolved using granular metadata transformation,
     * but just kept 'as is'
     */
    val kotlinModuleName: String?,

    /**
     * `null` when binary dependency was not resolved using granular metadata transformation,
     * but just kept 'as is'
     */
    val kotlinFragmentName: String?,
    val files: List<File>
) : IdeFragmentDependency()
