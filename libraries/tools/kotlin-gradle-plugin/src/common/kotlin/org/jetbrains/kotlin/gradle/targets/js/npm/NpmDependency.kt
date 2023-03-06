/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject.Companion.PACKAGE_JSON
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinCompilationNpmResolution
import java.io.File

data class NpmDependency(
    @Transient
    internal val project: Project?,
    private val name: String,
    private val version: String,
    val scope: Scope = Scope.NORMAL,
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

    override fun getGroup(): String? = null

    internal var parent: NpmDependency? = null
    internal val dependencies = mutableSetOf<NpmDependency>()
    internal var resolvedVersion: String? = null
    internal var integrity: String? = null

    override fun resolve(transitive: Boolean): Set<File> =
        mutableSetOf()

    override fun resolve(): MutableSet<File> {
        return mutableSetOf()
    }

    override fun resolve(context: DependencyResolveContext) {
    }

    val key: String = name

    override fun toString() = "$key: $version"

    override fun getFiles(): FileCollection = project!!.files()

    override fun getName() = name

    override fun getVersion() = version

    override fun getBuildDependencies(): TaskDependency = TaskDependency { mutableSetOf() }

    override fun contentEquals(dependency: Dependency) = this == dependency

    override fun getTargetComponentId() = DefaultLibraryBinaryIdentifier(project!!.path, key, "npm")

    override fun copy(): Dependency = this.copy(name = name)

    private var reason: String? = null

    override fun because(reason: String?) {
        this.reason = reason
    }

    override fun getReason(): String? = reason
}

internal fun directoryNpmDependency(
    project: Project,
    name: String,
    directory: File,
    scope: NpmDependency.Scope,
): NpmDependency {
    check(directory.isDirectory) {
        "Dependency on local path should point on directory but $directory found"
    }

    return NpmDependency(
        project = project,
        name = name,
        version = fileVersion(directory),
        scope = scope,
    )
}

fun String.isFileVersion() =
    startsWith(FILE_VERSION_PREFIX)

internal fun fileVersion(directory: File): String =
    "$FILE_VERSION_PREFIX${directory.canonicalPath}"

internal fun moduleName(directory: File): String {
    val packageJson = directory.resolve(PACKAGE_JSON)

    if (packageJson.isFile) {
        return fromSrcPackageJson(packageJson)!!.name
    }

    return directory.name
}

const val FILE_VERSION_PREFIX = "file:"