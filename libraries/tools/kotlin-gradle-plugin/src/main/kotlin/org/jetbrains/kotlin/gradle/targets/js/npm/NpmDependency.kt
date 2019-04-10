/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.FileCollectionDependency
import org.gradle.api.artifacts.SelfResolvingDependency
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.artifacts.dependencies.SelfResolvingDependencyInternal
import org.gradle.api.tasks.TaskDependency
import java.io.File
import org.gradle.internal.component.local.model.DefaultLibraryBinaryIdentifier
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmResolver.ResolutionCallResult.*

data class NpmDependency(
    private val project: Project,
    private val org: String?,
    private val name: String,
    private val version: String
) : SelfResolvingDependency,
    SelfResolvingDependencyInternal,
    FileCollectionDependency {
    override fun getGroup(): String? = org

    override fun resolve(): MutableSet<File> {
        val result = NpmResolver.resolve(project)

        return when (result) {
            is AlreadyInProgress -> mutableSetOf()
            is AlreadyResolved -> {
                check(this in result.resolution.dependencies) {
                    "Project hierarchy is already resolved in NPM without $this"
                }

                tryFindNodeModule()
            }
            is ResolvedNow -> {
                check(this in result.resolution.dependencies) {
                    "$this was not visited during resolution"
                }

                tryFindNodeModule()
            }
        }
    }

    private fun tryFindNodeModule(): MutableSet<File> {
        var p = project
        do {
            val result = p.rootDir.resolve("node_modules/$key")
            if (result.exists()) return mutableSetOf(result)
            p = project.parent ?: return mutableSetOf()
        } while (true)
    }

    val key: String = if (org == null) name else "@$org/$name"

    override fun toString() = "$key: $version"

    override fun resolve(transitive: Boolean): MutableSet<File> = resolve()

    override fun getFiles(): FileCollection = project.files(resolve())

    override fun getName() = name

    override fun getVersion() = version

    override fun getBuildDependencies(): TaskDependency = TaskDependency { mutableSetOf() }

    override fun contentEquals(dependency: Dependency) = this == dependency

    override fun getTargetComponentId() = DefaultLibraryBinaryIdentifier(project.path, key, "npm")

    override fun copy(): Dependency = this.copy(org = org)

    private var reason: String? = null

    override fun because(reason: String?) {
        this.reason = reason
    }

    override fun getReason(): String? = reason
}