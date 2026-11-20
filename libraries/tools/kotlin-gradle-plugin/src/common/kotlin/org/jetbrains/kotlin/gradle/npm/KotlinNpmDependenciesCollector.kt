/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.npm

import org.gradle.api.file.Directory
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import java.io.File
import javax.inject.Inject

internal class KotlinNpmDependenciesCollector @Inject constructor(
    private val objects: ObjectFactory,
    private val providers: ProviderFactory,
) {
    private val declarations: ListProperty<KotlinNpmDependency> =
        objects
            .listProperty(KotlinNpmDependency::class.java)

    val npmDependencies: Provider<List<KotlinNpmDependency>>
        get() = declarations

    fun add(
        name: String,
        version: String,
        dependencyType: KotlinNpmDependency.DependencyType,
    ) {
        declarations.add(
            KotlinNpmDependency(
                name = name,
                version = providers.provider { version },
                dependencyType = dependencyType,
            )
        )
    }

    fun add(
        name: String,
        version: Provider<String>,
        dependencyType: KotlinNpmDependency.DependencyType,
    ) {
        declarations.add(
            KotlinNpmDependency(
                name = name,
                version = version,
                dependencyType = dependencyType,
            )
        )
    }

    fun add(
        name: String,
        directory: File,
        dependencyType: KotlinNpmDependency.DependencyType,
    ) {
        add(
            name = name,
            directory = objects.directoryProperty().fileValue(directory),
            dependencyType = dependencyType,
        )
    }

    @JvmName("addDirectory")
    fun add(
        name: String,
        directory: Provider<Directory>,
        dependencyType: KotlinNpmDependency.DependencyType,
    ) {
        declarations.add(
            KotlinNpmDependency(
                name = name,
                directory = directory,
                dependencyType = dependencyType,
            )
        )
    }
}
