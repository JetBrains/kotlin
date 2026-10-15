/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

/**
 * The domain declaration as parsed from the 'domains.yaml' file
 */
internal data class DeclaredDomain(
    val name: String,

    /**
     * Files matching these patterns are included in this domain. Directory paths include all their descendants.
     * - e.g., 'compiler' will include the 'compiler' directory and all files under it
     * - e.g., '**​/​*gradle*' will include all paths whose name contains the word 'gradle'
     */
    val includes: List<String>,

    /**
     * Files matching these patterns are excluded from this domain. Directory paths exclude all their descendants.
     * See [includes].
     *
     * Note: 'include' and 'exclude' rules are each evaluated in declaration order: the last matching rule wins.
     * Rules starting with '!' are negated (e.g. '!foo/abc/bar' in 'exclude' revokes the exclusion of 'foo/abc/bar' by a previous rule).
     * 'exclude' rules can only narrow the set of included paths: they never add a path to the domain.
     * Rules matching a path take precedence over rules matching its parent directories.
     * e.g., a definition like
     * ```yaml
     * include:
     *     - foo
     * exclude:
     *     - foo/abc
     * ```
     *
     * Will exclude 'foo/abc/bar', as 'foo/abc' is matched before its parent directory 'foo'.
     */
    val excludes: List<String>,

    /**
     * Lists domains whose changes make all tests in this domain required for merging to master.
     * This relationship is not transitive: only changes in the listed domains trigger it.
     * The relationship is one-way: listing another domain here does not require that domain's tests for changes in this domain.
     */
    val mustRunAllTestsOnChangesIn: List<String>,
)
