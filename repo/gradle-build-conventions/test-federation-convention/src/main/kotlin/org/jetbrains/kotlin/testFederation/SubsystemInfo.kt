/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import org.gradle.api.Project
import java.nio.file.FileSystem
import java.nio.file.Path


/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


internal sealed interface SubsystemInfo {
    /**
     * @see DeclaredSubsystem.home
     */
    val home: String

    /**
     * The corresponsing [Subsystem] this info belongs to
     */
    val system: Subsystem

    /**
     * @see DeclaredSubsystem.includes
     */
    val include: List<String>

    /**
     * @see DeclaredSubsystem.excludes
     */
    val exclude: List<String>

    /**
     * @see DeclaredSubsystem.subsystems
     */
    val subsystems: List<SubsystemInfo>

    companion object
}

@JvmInline
value class RepositoryPath internal constructor(val value: Path) {
    val fileSystem: FileSystem get() = value.fileSystem

    init {
        require(!value.isAbsolute) { "Path must not be absolute" }
    }
}

internal fun Project.repositoryPath(path: Path): RepositoryPath {
    if (!path.isAbsolute) return RepositoryPath(path)
    return RepositoryPath(project.rootDir.toPath().relativize(path))
}


internal operator fun SubsystemInfo.contains(path: RepositoryPath): Boolean {
    return include.any { include -> path.fileSystem.getPathMatcher("glob:$include").matches(path.value) } &&
            exclude.none { exclude -> path.fileSystem.getPathMatcher("glob:${exclude}").matches(path.value) }
}

