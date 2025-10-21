/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.coverage

import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.dsl.kotlinJvmExtensionOrNull
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupCoroutine
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.utils.withType

internal val KotlinCoverageAction = KotlinProjectSetupCoroutine {
    KotlinPluginLifecycle.Stage.AfterEvaluateBuildscript.await()

    val jvmExtension = kotlinJvmExtensionOrNull
    val multiplatformExtension = multiplatformExtensionOrNull
    if (jvmExtension == null && multiplatformExtension == null) return@KotlinProjectSetupCoroutine

    val kgpVersion = getKotlinPluginVersion()

    jvmExtension?.target?.compilations?.all { compilation ->
        val dependencyProvider = provider {
            if (jvmExtension.coverage.enabled.get()) {
                listOf(dependencies.create("org.jetbrains.kotlin:coverage-compiler-plugin-embeddable:$kgpVersion"))
            } else {
                emptyList()
            }
        }
        compilation.internal.configurations.pluginConfiguration.dependencies.addAllLater(dependencyProvider)
    }

    multiplatformExtension?.targets
        ?.filter { it.platformType == KotlinPlatformType.jvm || it.platformType == KotlinPlatformType.androidJvm }
        ?.forEach { target ->
            target.compilations.all { compilation ->
                if (compilation.name.contains("test", ignoreCase = true)) return@all

                val dependencyProvider = provider {
                    if (multiplatformExtension.coverage.enabled.get()) {
                        listOf(dependencies.create("org.jetbrains.kotlin:coverage-compiler-plugin-embeddable:$kgpVersion"))
                    } else {
                        emptyList()
                    }
                }
                compilation.internal.configurations.pluginConfiguration.dependencies.addAllLater(dependencyProvider)
            }
        }

    tasks.withType<KotlinCompile>().configureEach { compileTask ->
        compileTask.pluginClasspath.from()
    }

    KotlinPluginLifecycle.Stage.AfterEvaluateBuildscript.await()


    if (jvmExtension != null) {
        dependencies.add("implementation", "org.jetbrains.kotlin:coverage-runtime:$kgpVersion")
    } else if (multiplatformExtension != null) {
        dependencies.add("jvmMainImplementation", "org.jetbrains.kotlin:coverage-runtime:$kgpVersion")
    } else {
        return@KotlinProjectSetupCoroutine
    }

    tasks.withType<Test>().configureEach { testTask ->
        val reportPath = layout.buildDirectory.dir("kover/exec/${testTask.name}").get().asFile.absolutePath
        testTask.systemProperty("kotlin.coverage.executions.path", reportPath)
        testTask.doFirst {
            file(reportPath).deleteRecursively()
        }
    }
    tasks.withType<KotlinCompile>().configureEach { compileTask ->
        compileTask.pluginClasspath.from()

        val moduleName = compileTask.compilerOptions.moduleName.get()
        val metadataFilePath = layout.buildDirectory.file("kover/metadata/${moduleName}.kim").get().asFile.absolutePath
        compileTask.compilerOptions.freeCompilerArgs.addAll(
            "-P", "plugin:org.jetbrains.kotlin.coverage:modulePath=${projectDir.absolutePath}",
            "-P", "plugin:org.jetbrains.kotlin.coverage:metadataFilePath=$metadataFilePath"
        )
    }
}