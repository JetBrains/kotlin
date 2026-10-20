/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.wasmtools

import SystemPropertyClasspathProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.register
import java.io.File

/**
 * Provisions [wasm-tools](https://github.com/bytecodealliance/wasm-tools) from the `wasmToolsDistributions()` repository
 * and passes it to tests as the `wasm.tools.path` system property.
 */
abstract class WasmToolsExtension(private val project: Project) {
    val wasmToolsVersion: String
        get() = project.extensions.getByType<VersionCatalogsExtension>()
            .named("libs")
            .findVersion("wasmTools")
            .get().requiredVersion

    private val isWindows = OperatingSystem.current().isWindows

    private val hostClassifier: String = run {
        val os = OperatingSystem.current()
        val osName = when {
            os.isMacOsX -> "macos"
            os.isWindows -> "windows"
            os.isLinux -> "linux"
            else -> error("unsupported os type $os")
        }
        val arch = when (val arch = project.providers.systemProperty("os.arch").get().lowercase()) {
            "aarch64", "arm64" -> "aarch64"
            "x86_64", "amd64" -> "x86_64"
            else -> error("unsupported architecture $arch")
        }
        "$arch-$osName"
    }

    private fun dependencyNotation(classifier: String): String {
        val extension = if (classifier.endsWith("-windows")) "zip" else "tar.gz"
        return "bytecodealliance.wasm-tools:wasm-tools:$wasmToolsVersion:$classifier@$extension"
    }

    private val wasmTools: Configuration = project.configurations.create("wasmTools") {
        isCanBeResolved = true
        isCanBeConsumed = false
    }

    init {
        project.dependencies.add(wasmTools.name, dependencyNotation(hostClassifier))

        project.configurations.matching { it.name == "implicitDependencies" }.configureEach {
            for (classifier in listOf("x86_64-linux", "aarch64-macos", "x86_64-windows")) { 
                project.dependencies.add(name, dependencyNotation(classifier))
            }
        }
    }

    private val unpackWasmTools: TaskProvider<Copy> = project.tasks.register<Copy>("unpackWasmTools") {
        dependsOn(wasmTools)
        val archive = wasmTools.elements.map { it.single().asFile }
        from(if (isWindows) project.zipTree(archive) else project.tarTree(archive))
        into(project.layout.buildDirectory.dir("tools/wasm-tools"))
    }

    val wasmToolsExecutable: Provider<File> = run {
        val executablePath = "wasm-tools-$wasmToolsVersion-$hostClassifier/" + if (isWindows) "wasm-tools.exe" else "wasm-tools"
        unpackWasmTools.map { it.destinationDir.resolve(executablePath) }
    }

    fun Test.setupWasmTools() {
        jvmArgumentProviders += project.objects.newInstance<SystemPropertyClasspathProvider>().apply {
            classpath.from(wasmToolsExecutable)
            property.set("wasm.tools.path")
        }
    }
}
