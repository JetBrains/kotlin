/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.npm.internal

import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.nodeJsRoot
import org.jetbrains.kotlin.gradle.targets.js.nodejs.JsPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.js.webTargetVariant
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.web.npm.isolated.isIsolatedNpmResolutionEnabled

/**
 * Directory layout of the npm root project.
 *
 * Contains no infrastructure-specific logic: both the legacy (root project based) and
 * the Isolated Projects compatible NPM resolution compute the very same relative paths,
 * they only differ in the project that owns the `build` directory.
 */
internal class NpmProjectLayout(
    /**
     * Root directory of the npm root project.
     * Contains subdirectories of npm workspaces for each Kotlin compilation.
     */
    val rootPackageDirectory: Provider<Directory>,
) {
    val projectPackagesDirectory: Provider<Directory>
        get() = rootPackageDirectory.map { it.dir(PACKAGES) }

    val nodeModulesGradleCacheDirectory: Provider<Directory>
        get() = rootPackageDirectory.map { it.dir(PACKAGES_IMPORTED) }

    val nodeModulesDirectory: Provider<Directory>
        get() = rootPackageDirectory.map { it.dir(NpmProject.NODE_MODULES) }

    companion object {
        const val PACKAGES: String = "packages"
        const val PACKAGES_IMPORTED: String = "packages_imported"
    }
}

/**
 * The only place where the owner of the npm root project directory is decided:
 * the legacy pipeline uses the root project's `build` directory,
 * while the Isolated Projects compatible pipeline uses the project-local one.
 */
internal val KotlinJsIrCompilation.npmProjectLayout: NpmProjectLayout
    get() = if (project.isIsolatedNpmResolutionEnabled) {
        val rootDirectoryName = webTargetVariant(
            jsVariant = JsPlatformDisambiguator.jsPlatform,
            wasmVariant = WasmPlatformDisambiguator.platformDisambiguator,
        )
        NpmProjectLayout(project.layout.buildDirectory.dir(rootDirectoryName))
    } else {
        NpmProjectLayout(nodeJsRoot().rootPackageDirectory)
    }
