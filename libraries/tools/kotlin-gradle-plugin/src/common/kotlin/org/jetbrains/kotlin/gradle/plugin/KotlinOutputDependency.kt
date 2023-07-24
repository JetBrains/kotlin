/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.FileCollectionDependency
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.artifacts.dependencies.SelfResolvingDependencyInternal
import org.gradle.api.tasks.TaskDependency
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropIdentifier
import java.io.File

/**
 * Identifier used for [KotlinOutputDependency].
 * All use cases where Kotlin needs to add 'file collection based' dependencies shall be modelled
 * using [KotlinOutputDependency] where this identifier is able to provide
 * further information about 'what kind of dependency' this is used.
 *
 * This identifier basically contains
 * 1) The information that this dependency was added by the Kotlin Gradle Plugin
 * 2) The information that the files are locally built
 * 3) The information about the semantics of the dependency
 */
internal sealed class KotlinOutputDependencyIdentifier : ComponentIdentifier {

    /**
     * Indicates that the dependency is representing the compilation under
     * the specified coordinates and that the reason for the dependency is a 'compilation association'
     *
     * e.g. 'test compilations' are associated with 'main compilations'
     */
    internal class AssociateCompilation(
        val projectPath: String, val target: String, val compilation: String,
    ) : KotlinOutputDependencyIdentifier() {
        constructor(compilation: KotlinCompilation<*>) : this(
            projectPath = compilation.project.path,
            target = compilation.target.targetName,
            compilation = compilation.compilationName
        )
    }

    /**
     * Indicates that the dependency represents the output of the 'cinterop tool' for a single cinterop,
     * identified by the specified [identifier]
     */
    internal class CInterop(
        val identifier: CInteropIdentifier,
    ) : KotlinOutputDependencyIdentifier()

    override fun getDisplayName(): String = when (this) {
        is AssociateCompilation -> "Associate Compilation: '$projectPath/$target/$compilation'"
        is CInterop -> "CInterop ${identifier.uniqueName}"
    }

    override fun toString(): String = displayName
}

/**
 * Special 'Dependency' implementation that shall be used to add 'file collection' based dependencies
 * to Kotlin entities (such as compilations). This dependency implementation retains information about
 * the semantics of the file collection underneath (by providing a [KotlinOutputDependencyIdentifier])
 */
internal class KotlinOutputDependency internal constructor(
    private val identifier: KotlinOutputDependencyIdentifier,
    private val files: FileCollection,
) : FileCollectionDependency, SelfResolvingDependencyInternal {
    private var reason: String? = null

    override fun getTargetComponentId(): ComponentIdentifier = identifier
    override fun getGroup(): String? = null
    override fun getName(): String = "unspecified"
    override fun getVersion(): String? = null

    override fun contentEquals(dependency: Dependency): Boolean {
        if (dependency !is KotlinOutputDependency) return false
        return files == dependency.files
    }

    override fun copy(): KotlinOutputDependency {
        return KotlinOutputDependency(identifier, files)
    }

    override fun getReason(): String? = reason

    override fun because(reason: String?) {
        this.reason = reason
    }

    override fun getBuildDependencies(): TaskDependency {
        return files.buildDependencies
    }

    override fun resolve(): MutableSet<File> {
        return files.files
    }

    override fun resolve(transitive: Boolean): MutableSet<File> {
        return files.files
    }

    override fun getFiles(): FileCollection {
        return files
    }
}
