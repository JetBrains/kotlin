/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import org.gradle.api.Project
import java.nio.file.FileSystem
import java.nio.file.Path
import kotlin.io.path.isDirectory

internal sealed interface DomainInfo {
    /**
     * @see DeclaredDomain.home
     */
    val home: String

    /**
     * The corresponsing [Domain] this info belongs to
     */
    val domain: Domain

    /**
     * @see DeclaredDomain.includes
     */
    val include: List<String>

    /**
     * @see DeclaredDomain.excludes
     */
    val exclude: List<String>

    /**
     * @see DeclaredDomain.fullyAffectedBy
     */
    val fullyAffectedBy: List<DomainInfo>

    companion object
}

@JvmInline
value class RepositoryPath internal constructor(val value: Path) {
    val fileSystem: FileSystem get() = value.fileSystem

    override fun toString(): String {
        return value.toString()
    }

    init {
        require(!value.isAbsolute) { "Path must not be absolute" }
    }
}

internal fun Project.repositoryPath(path: Path): RepositoryPath {
    if (!path.isAbsolute) return RepositoryPath(path)
    return RepositoryPath(project.rootDir.toPath().relativize(path))
}


internal operator fun DomainInfo.contains(path: RepositoryPath): Boolean {
    val value = if (path.value.isDirectory()) path.value.resolve(".") else path.value

    return include.any { include -> path.fileSystem.getPathMatcher("glob:$include").matches(value) } &&
            exclude.none { exclude -> path.fileSystem.getPathMatcher("glob:${exclude}").matches(value) }
}

val RepositoryPath.domain: Domain
    get() = DomainInfo.all.firstOrNull { subsystem -> this in subsystem }?.domain ?: Domain.Unknown
