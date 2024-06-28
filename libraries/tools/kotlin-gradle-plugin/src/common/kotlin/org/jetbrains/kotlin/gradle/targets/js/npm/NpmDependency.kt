/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.FileCollectionDependency
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.artifacts.dependencies.SelfResolvingDependencyInternal
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.TaskDependency
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject.Companion.PACKAGE_JSON
import java.io.File

data class NpmDependency(
    val objectFactory: ObjectFactory,
    val scope: Scope = Scope.NORMAL,
    private val name: String,
    private val version: String,
) : FileCollectionDependency,
    SelfResolvingDependencyInternal {

    enum class Scope {
        NORMAL,
        DEV,
        OPTIONAL,
        PEER
    }

    private var reason: String? = null

    override fun getGroup(): String? = null

    override fun getName() = name

    override fun getVersion() = version

    override fun resolve(transitive: Boolean): Set<File> =
        resolve()

    override fun getTargetComponentId(): ComponentIdentifier? = null
    override fun resolve(): MutableSet<File> = mutableSetOf()

    override fun getFiles(): FileCollection = objectFactory.fileCollection()

    override fun getBuildDependencies(): TaskDependency = TaskDependency { mutableSetOf() }

    override fun contentEquals(dependency: Dependency) = this == dependency

    override fun copy(): Dependency = this.copy(name = name)

    override fun because(reason: String?) {
        this.reason = reason
    }

    override fun getReason(): String? = reason
}

internal fun directoryNpmDependency(
    objectFactory: ObjectFactory,
    scope: NpmDependency.Scope,
    name: String,
    directory: File,
): NpmDependency {
    check(directory.isDirectory) {
        "Dependency on local path should point on directory but $directory found"
    }

    return NpmDependency(
        objectFactory = objectFactory,
        scope = scope,
        name = name,
        version = fileVersion(directory),
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