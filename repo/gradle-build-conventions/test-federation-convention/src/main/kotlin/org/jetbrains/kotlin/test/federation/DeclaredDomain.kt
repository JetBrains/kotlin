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
     * Note: If a file matches an 'include' and 'exclude' pattern, then the 'most specific' pattern will dominate.
     * e.g., a definition like
     * ```yaml
     * include:
     *     - foo
     * exclude:
     *     - foo/abc
     * ```
     *
     * Will exclude 'foo/abc/bar', as the exclude rule is considered 'more specific'.
     */
    val excludes: List<String>,

    /**
     * Lists domains whose changes make all tests in this domain required for merging to master.
     * This relationship is not transitive: only changes in the listed domains trigger it.
     * The relationship is one-way: listing another domain here does not require that domain's tests for changes in this domain.
     */
    val mustRunAllTestsOnChangesIn: List<String>,
)
