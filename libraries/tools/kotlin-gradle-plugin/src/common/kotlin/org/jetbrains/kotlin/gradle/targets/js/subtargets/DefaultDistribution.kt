/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.subtargets

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.distsDirectory
import org.jetbrains.kotlin.gradle.targets.js.dsl.Distribution
import org.jetbrains.kotlin.gradle.targets.js.dsl.Distribution.Companion.DIST
import org.jetbrains.kotlin.gradle.utils.property
import java.io.File

internal fun createDefaultDistribution(
    project: Project,
    targetName: String,
    name: String? = null
) =
    DefaultDistribution(project, project.objects.property(targetName), project.objects.property(name))

internal fun createDefaultDistribution(
    project: Project,
    targetName: String,
    name: Property<String>?
) =
    DefaultDistribution(project, project.objects.property(targetName), project.objects.property<String>().apply {
        if (name != null) {
            value(name)
        }
    })

class DefaultDistribution(
    private val project: Project,
    private val targetName: Property<String>,
    override val distributionName: Property<String>,
) : Distribution {
    @Deprecated("Use `distributionName` instead", ReplaceWith("distributionName"))
    override var name: String?
        get() = distributionName.orNull
        set(value) {
            distributionName.set(value)
        }

    @Deprecated("Use `outputDirectory` instead", ReplaceWith("outputDirectory"))
    override var directory: File
        get() = outputDirectory.get().asFile
        set(value) {
            outputDirectory.set(value)
        }

    override val outputDirectory: DirectoryProperty = project.objects.directoryProperty().convention(
        distributionName.flatMap { project.layout.buildDirectory.dir("$DIST/${targetName.get()}/$it") }.orElse(project.distsDirectory)
    )
}
