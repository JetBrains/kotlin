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
     * Files matching these patterns will be included in this subsystem. Directory paths include all their descendants.
     * - e.g., 'compiler' will include the 'compiler' directory and all files under it
     * - e.g., '**​/​*gradle*' will include all paths whose name contains the word 'gradle'
     */
    val includes: List<String>,

    /**
     * Files matching these patterns will be excluded from this subsystem. Directory paths exclude all their descendants.
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
     * [Domain] names with which this domain declares a full-domain contract.
     * This domain's tests run in full mode when any contracted domain is affected. Contracts are not transitive.
     */
    val contract: List<String>,
)
