/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.ecosystem.npm

import org.jetbrains.kotlin.gradle.ExperimentalNpmDependencyManagementDsl

/**
 * Whether KGP installs the npm dependencies of the projects of the build.
 *
 * See [NpmDependencyManagementDsl.sharedNpmDependenciesMode].
 *
 * @since 2.5.20
 */
@ExperimentalNpmDependencyManagementDsl
enum class SharedNpmDependenciesMode {
    /**
     * Collects the npm dependencies into one npm project per build in the root project,
     * which gives one npm install and one lockfile.
     */
    SHARED_IN_ROOT_PROJECT,

    /**
     * Installs nothing, so you must install the npm dependencies yourself.
     */
    DISABLED,
}
