/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.npm

import org.gradle.api.file.Directory
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.targets.js.npm.NPM_DEP_FILE_VERSION_PREFIX
import org.jetbrains.kotlin.gradle.utils.normalizedAbsoluteFile
import java.io.File
import javax.inject.Inject

internal class KotlinNpmDependenciesCollector @Inject constructor(
    objectFactory: ObjectFactory,
) {
    private val declarations: ListProperty<DefaultKotlinNpmDependency> =
        objectFactory
            .listProperty(DefaultKotlinNpmDependency::class.java)

    val npmDependencies: Provider<List<DefaultKotlinNpmDependency>>
        get() = declarations.map { it.distinctBy { dependency -> dependency.name } }

    fun add(
        name: String,
        version: String,
        scope: KotlinNpmDependency.Scope,
    ) {
        declarations.add(
            DefaultKotlinNpmDependency(
                name = name,
                version = version,
                scope = scope,
            )
        )
    }

    fun add(
        name: Provider<String>,
        version: Provider<String>,
        scope: KotlinNpmDependency.Scope,
    ) {
        declarations.add(
            name.zip(version) { packageName, npmVersion ->
                DefaultKotlinNpmDependency(
                    name = packageName,
                    version = npmVersion,
                    scope = scope,
                )
            }
        )
    }

    fun add(
        name: String,
        directory: Directory,
        scope: KotlinNpmDependency.Scope,
    ) {
        add(
            name = name,
            file = directory.asFile,
            scope = scope,
        )
    }

    fun add(
        name: String,
        file: File,
        scope: KotlinNpmDependency.Scope,
    ) {
        add(
            name = name,
            version = file.npmFileNotation(),
            scope = scope,
        )
    }

    fun addDirectory(
        name: String,
        directory: Provider<Directory>,
        scope: KotlinNpmDependency.Scope,
    ) {
        declarations.add(
            directory.map { dir ->
                DefaultKotlinNpmDependency(
                    name = name,
                    version = dir.asFile.npmFileNotation(),
                    scope = scope,
                )
            }
        )
    }
}

/**
 * The npm [local path](https://docs.npmjs.com/cli/v11/configuring-npm/package-json#local-paths)
 * notation of this file, to be used as an npm dependency version.
 */
private fun File.npmFileNotation(): String =
    "$NPM_DEP_FILE_VERSION_PREFIX${normalizedAbsoluteFile()}"
