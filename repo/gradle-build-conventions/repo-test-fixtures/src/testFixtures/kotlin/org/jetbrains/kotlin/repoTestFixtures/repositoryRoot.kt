/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.repoTestFixtures

import org.jetbrains.kotlin.testFederation.RepositoryPath
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolute

internal val repositoryRoot by lazy {
    findRepositoryRoot(Path("").absolute())
        ?: error("Cannot find the repository root from the current working directory")
}

/**
 * Converts an absolute or working-directory-relative path to a normalized repository path without accessing the filesystem.
 * The path need not exist (for example, a deleted file listed in a dump).
 * @throws IllegalArgumentException if the normalized path is outside the repository root.
 */
fun Path.toRepositoryPath(): RepositoryPath {
    val absolutePath = absolute().normalize()
    require(absolutePath.startsWith(repositoryRoot)) {
        "Path '$this' is not within the repository root '$repositoryRoot'"
    }
    return RepositoryPath(repositoryRoot, repositoryRoot.relativize(absolutePath))
}

/** See [Path.toRepositoryPath]. */
fun File.toRepositoryPath(): RepositoryPath {
    return toPath().toRepositoryPath()
}


/**
 * Constructs a [RepositoryPath] relative to the repository root found from the current working directory.
 * Unlike [Path.toRepositoryPath], [path] is repository-relative, not working-directory-relative.
 * The path need not exist and is not normalized.
 *
 * @param path a path relative to the repository root.
 * @throws IllegalArgumentException if [path] is absolute.
 */
fun repositoryPath(path: String): RepositoryPath {
    val path = Path(path)
    require(!path.isAbsolute) { "Path '$path' is not relative" }
    return RepositoryPath(repositoryRoot, path)
}
