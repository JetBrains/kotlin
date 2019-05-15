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
import org.gradle.api.internal.artifacts.DependencyResolveContext
import org.gradle.api.internal.artifacts.ResolvableDependency
import org.gradle.api.internal.artifacts.dependencies.SelfResolvingDependencyInternal
import org.gradle.api.tasks.TaskDependency
import org.gradle.internal.component.local.model.DefaultLibraryBinaryIdentifier
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmResolver.ResolutionCallResult.*
import java.io.File

data class NpmDependency(
    internal val project: Project,
    private val org: String?,
    private val name: String,
    private val version: String
) : SelfResolvingDependency,
    SelfResolvingDependencyInternal,
    ResolvableDependency,
    FileCollectionDependency {
    override fun getGroup(): String? = org

    internal val dependencies = mutableSetOf<NpmDependency>()

    override fun resolve(): MutableSet<File> {
        resolveProject() ?: return mutableSetOf()

        val all = mutableSetOf<File>()
        val visited = mutableSetOf<NpmDependency>()

        fun visit(item: NpmDependency) {
            if (item in visited) return
            visited.add(item)
            item.project.npmProject.resolve(item.key)?.let {
                all.add(it)
            }
        }

        visit(this)

        return all
    }

    private fun resolveProject(): NpmResolver.ResolvedProject? {
        val result = NpmResolver.resolve(project)

        return when (result) {
            is AlreadyInProgress -> null
            is AlreadyResolved -> {
                check(this in result.resolution.npmPackage!!.npmDependencies) {
                    "Project hierarchy is already resolved in NPM without $this"
                }

                result.resolution
            }
            is ResolvedNow -> {
                check(this in result.resolution.npmPackage!!.npmDependencies) {
                    "$this was not visited during resolution"
                }

                result.resolution
            }
        }
    }

    val key: String = if (org == null) name else "@$org/$name"

    override fun toString() = "$key: $version"

    override fun resolve(context: DependencyResolveContext) {
        resolve()
        dependencies.forEach {
            context.add(it)
        }
    }

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