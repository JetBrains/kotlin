/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(InternalKotlinGradlePluginApi::class)

package org.jetbrains.kotlin.build.swc

import kotlinBuildProperties
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.testing.Test
import org.gradle.process.CommandLineArgumentProvider
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.swc.SwcEnvSpec
import java.io.File

abstract class SwcExtension(
    private val project: Project,
    private val swc: SwcEnvSpec,
) {
    val swcVersion: Provider<String>
        get() = project.kotlinBuildProperties.versionsProperty("swc")

    val swcExecutablePath: Provider<String> = swc.executable

    fun Test.setupSwc() {
        with(swc) {
            dependsOn(project.swcSetupTaskProvider)
        }

        val swcPathProvider = project.objects.newInstance(SwcPathArgumentProvider::class.java)
        swcPathProvider.executablePath.set(swcExecutablePath)
        jvmArgumentProviders.add(swcPathProvider)
    }
}

/**
 * Passes the absolute path of the SWC executable to the test JVM as the `swc.path` system property.
 *
 * The absolute path lives in the Gradle user home and is machine-specific, so it is not tracked as an input.
 * Only the executable file name (`swc-<version>-<platform classifier>`) is tracked, which keeps the test task relocatable
 * while still invalidating it when the SWC version or platform changes.
 */
abstract class SwcPathArgumentProvider : CommandLineArgumentProvider {
    @get:Internal
    abstract val executablePath: Property<String>

    @get:Input
    val executableName: Provider<String>
        get() = executablePath.map { File(it).name }

    override fun asArguments(): Iterable<String> = listOf("-Dswc.path=${executablePath.get()}")
}
