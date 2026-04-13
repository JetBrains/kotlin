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
     * The 'home' directory of the subsystem.
     * The home directory is used for storing contract dumps.
     * The directory must be a relative path to from the repository root.
     * Note: The home directory is not automatically included!
     */
    val home: String,

    /**
     * Files matching these 'glob' patterns will be included in this subsystem.
     * - e.g., 'compiler/​**' will include all files under the 'compiler' subdirectory
     * - e.g., '**​/​*gradle* will include all files containing the word 'gradle'
     */
    val includes: List<String>,

    /**
     * Files matching these 'glob' patterns will be excluded from this subsystem
     * See [includes].
     *
     * Note: If a file matches an 'include' and 'exclude' glob pattern, then the 'most specific' pattern will dominate.
     * e.g., a definition like
     * ```yaml
     * include:
     *     - foo/​**
     * exclude:
     *     - foo/​**​/bar
     * ```
     *
     * Will exclude 'foo/abc/bar', as the exclude rule is considered 'more specific'.
     */
    val excludes: List<String>,

    /**
     * List of [Domain] names which this domain declares as dependency (fully affected by)
     * Domains are marked as affected if any of its dependencies (even transitively) are marked as affected.
     * e.g., a backend like 'Wasm' will be affected if anything in the larger 'Compiler' domain was changed, but
     * the larger 'Compiler' domain is not affected if changes are only located within wasm
     */
    val fullyAffectedBy: List<String>,
)
