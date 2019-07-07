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
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinCompilationNpmResolution
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import java.io.File

data class NpmDependency(
    internal val project: Project,
    private val org: String?,
    private val name: String,
    private val version: String,
    val scope: Scope = Scope.NORMAL
) : SelfResolvingDependency,
    SelfResolvingDependencyInternal,
    ResolvableDependency,
    FileCollectionDependency {

    enum class Scope {
        NORMAL,
        DEV,
        OPTIONAL,
        PEER
    }

    override fun getGroup(): String? = org

    internal var parent: NpmDependency? = null
    internal val dependencies = mutableSetOf<NpmDependency>()
    internal var resolvedVersion: String? = null
    internal var integrity: String? = null

    fun getDependenciesRecursively(): Set<NpmDependency> {
        val visited = mutableSetOf<NpmDependency>()

        fun visit(it: NpmDependency) {
            if (!visited.add(it)) return

            it.dependencies.forEach { child ->
                visit(child)
            }
        }

        visit(this)

        return visited
    }

    override fun resolve(transitive: Boolean): Set<File> {
        val npmPackage = resolveProject() ?: return mutableSetOf()
        val npmProject = npmPackage.npmProject

        val all = mutableSetOf<File>()
        val visited = mutableSetOf<NpmDependency>()

        fun visit(item: NpmDependency) {
            if (item in visited) return
            visited.add(item)

            npmProject.resolve(item.key)?.let {
                if (it.isFile) all.add(it)
                if (it.path.endsWith(".js")) {
                    val baseName = it.path.removeSuffix(".js")
                    val metaJs = File(baseName + ".meta.js")
                    if (metaJs.isFile) all.add(metaJs)
                    val kjsmDir = File(baseName)
                    if (kjsmDir.isDirectory) {
                        kjsmDir.walkTopDown()
                            .filter { it.extension == "kjsm" }
                            .forEach { all.add(it) }
                    }
                }
            }

            if (transitive) {
                item.dependencies.forEach {
                    visit(it)
                }
            }
        }

        visit(this)

        return all
    }

    override fun resolve(): MutableSet<File> {
        val npmPackage = parent?.resolveProject()
            ?: resolveProject()
            ?: return mutableSetOf()
        return mutableSetOf(npmPackage.npmProject.resolve(key)!!)
    }

    override fun resolve(context: DependencyResolveContext) {
        val npmPackage = resolveProject()
        if (npmPackage != null) {
            npmPackage.project.files(npmPackage.npmProject.resolve(key))
            dependencies.forEach {
                context.add(it)
            }
        }
    }

    // may return null only during npm resolution
    // (it can be called since NpmDependency added to configuration that
    // requires resolve to build package.json, in this case we should just skip this call)
    private fun resolveProject(): KotlinCompilationNpmResolution? {
        val nodeJs = NodeJsRootPlugin.apply(project.rootProject)
        return nodeJs.npmResolutionManager.getNpmDependencyResolvedCompilation(this)
    }

    val key: String = if (org == null) name else "@$org/$name"

    override fun toString() = "$key: $version"

    override fun getFiles(): FileCollection = project.files(resolve(true))

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