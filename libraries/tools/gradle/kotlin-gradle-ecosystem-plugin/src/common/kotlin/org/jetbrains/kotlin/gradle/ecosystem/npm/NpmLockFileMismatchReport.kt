/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.ecosystem.npm

import org.jetbrains.kotlin.gradle.ExperimentalNpmDependencyManagementDsl

/**
 * What happens when the lockfile resolved by npm install differs from the stored one.
 *
 * See [NpmDependencyManagementDsl.LockFileDsl.mismatchReport].
 *
 * @since 2.5.20
 */
@ExperimentalNpmDependencyManagementDsl
enum class NpmLockFileMismatchReport {
    /**
     * Ignores the mismatch.
     */
    NONE,

    /**
     * Logs a warning and continues the build.
     */
    WARNING,

    /**
     * Fails the build.
     */
    FAIL,
}
