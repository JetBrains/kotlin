/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.gradle.targets.web.npm.internal

import org.gradle.api.file.ArchiveOperations
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependencyDeclaration
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJson
import org.jetbrains.kotlin.gradle.targets.js.npm.fromSrcPackageJson
import org.jetbrains.kotlin.gradle.targets.js.npm.isCompatibleArchive
import java.io.File

/**
 * Collects the npm dependencies declared by the `package.json` files embedded into the Kotlin/JS
 * libraries of [classpath].
 *
 * A Kotlin/JS library such as `kotlinx-datetime` is a regular klib that may carry its own
 * `package.json` inside the archive, declaring the npm packages its JavaScript part needs at runtime.
 *
 * Note: this is a temporary solution.
 * The legacy NPM resolution unpacks every such library into a separate "imported" npm module
 * (see `GradleNodeModuleBuilder`) and registers it as an additional npm workspace.
 * Here the embedded declarations are merged into the `package.json` of the consuming compilation instead,
 * which is enough as long as all the npm dependencies of the build are hoisted
 * into a single `node_modules` of the shared npm root project.
 */
internal fun collectEmbeddedNpmDependencies(
    archiveOperations: ArchiveOperations,
    classpath: Iterable<File>,
): List<NpmDependencyDeclaration> =
    classpath
        .mapNotNull { file -> file.embeddedPackageJson(archiveOperations) }
        .flatMap { packageJson -> packageJson.npmDependencyDeclarations() }

private fun File.embeddedPackageJson(archiveOperations: ArchiveOperations): PackageJson? = when {
    !isFile -> null
    name == NpmProject.PACKAGE_JSON -> fromSrcPackageJson(this)
    isCompatibleArchive -> {
        val packageJsonEntry = archiveOperations.zipTree(this)
            .firstOrNull { it.name == NpmProject.PACKAGE_JSON }
        fromSrcPackageJson(packageJsonEntry)
    }
    else -> null
}

private fun PackageJson.npmDependencyDeclarations(): List<NpmDependencyDeclaration> = buildList {
    dependencies.mapTo(this) { (name, version) ->
        NpmDependencyDeclaration(NpmDependency.Scope.NORMAL, name, version)
    }
    peerDependencies.mapTo(this) { (name, version) ->
        NpmDependencyDeclaration(NpmDependency.Scope.PEER, name, version)
    }
    optionalDependencies.mapTo(this) { (name, version) ->
        NpmDependencyDeclaration(NpmDependency.Scope.OPTIONAL, name, version)
    }
    // `devDependencies` are intentionally ignored: they are not needed to run the consuming compilation.
}
