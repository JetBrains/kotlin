/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.repoTestFixtures

import org.jetbrains.kotlin.testFederation.RepositoryPath
import org.jetbrains.kotlin.testFederation.isTestFederationIgnored
import java.io.File
import java.nio.file.Path

/** Classifies why a repository path can be excluded from checks or test execution. */
enum class IgnoreLevel {
    /**
     * The file is checked into our repository, but we can 'fast track' changes within this file without running any tests.
     */
    TestFederationIgnored,

    /**
     * The file is ignored in git
     */
    GitIgnored
}


/**
 * Returns whether test federation rules allow skipping tests for this path.
 * Relative paths are resolved from the current working directory.
 * @throws IllegalArgumentException if the path is not within the repository root.
 */
fun Path.isTestFederationIgnored(): Boolean = this.toRepositoryPath().isTestFederationIgnored

/** See [Path.isTestFederationIgnored]. */
fun File.isTestFederationIgnored(): Boolean = this.toRepositoryPath().isTestFederationIgnored

/**
 * Returns true if the path is ignored (as in listed in any '.gitignore').
 * Relative paths are resolved from the current working directory.
 * @throws IllegalArgumentException if the path is not within the repository root.
 */
fun Path.isGitIgnored(): Boolean = toRepositoryPath().isGitIgnored()

/** See [Path.isGitIgnored]. */
fun File.isGitIgnored(): Boolean = toPath().isGitIgnored()

/**
 * Returns the [ignore level][RepositoryPath.ignoreLevel] for this path.
 * Relative paths are resolved from the current working directory.
 * @throws IllegalArgumentException if the normalized path is outside the repository root.
 */
val Path.ignoreLevel: IgnoreLevel?
    get() = toRepositoryPath().ignoreLevel

/**
 * Returns why this path is ignored, or `null` if neither Git nor test federation rules ignore it.
 * [IgnoreLevel.GitIgnored] takes precedence over [IgnoreLevel.TestFederationIgnored].
 * Git ignore rules are evaluated relative to this path's [RepositoryPath.root].
 *
 * @throws IllegalArgumentException if the normalized path is outside its repository root.
 */
val RepositoryPath.ignoreLevel: IgnoreLevel?
    get() = when {
        isGitIgnored() -> IgnoreLevel.GitIgnored
        isTestFederationIgnored -> IgnoreLevel.TestFederationIgnored
        else -> null
    }

/**
 * See [Path.ignoreLevel]
 */
val File.ignoreLevel: IgnoreLevel?
    get() = toPath().ignoreLevel
