/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.d8

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.targets.js.AbstractSettings
import org.jetbrains.kotlin.gradle.utils.property

@OptIn(ExperimentalWasmDsl::class)
open class D8RootExtension(
    @Transient val project: Project,
    private val d8EnvSpec: D8EnvSpec,
) : AbstractSettings<D8Env>() {

    private val gradleHome = project.gradle.gradleUserHomeDir.also {
        project.logger.kotlinInfo("Storing cached files in $it")
    }

    override val downloadProperty: org.gradle.api.provider.Property<Boolean> = project.objects.property<Boolean>()
        .convention(true)

    // value not convention because this property can be nullable to not add repository
    override val downloadBaseUrlProperty: org.gradle.api.provider.Property<String> = project.objects.property<String>()
        .value("https://storage.googleapis.com/chromium-v8/official/canary")

    override val installationDirectory: DirectoryProperty = project.objects.directoryProperty()
        .fileValue(gradleHome.resolve("d8"))

    /**
     * The same as in [D8EnvSpec.version]
     */
    override val versionProperty: org.gradle.api.provider.Property<String> = project.objects.property<String>()
        .convention("13.4.61")

    /**
     * Specify the edition of the D8.
     *
     * Valid options for bundled version are `rel` (release variant) and `dbg` (debug variant).
     */
    val edition: org.gradle.api.provider.Property<String> = project.objects.property<String>()
        .convention("rel")

    override val commandProperty: org.gradle.api.provider.Property<String> = project.objects.property<String>()
        .convention("d8")

    override fun finalizeConfiguration(): D8Env {
        return d8EnvSpec.env.get()
    }

    val setupTaskProvider: TaskProvider<out D8SetupTask>
        get() = with(d8EnvSpec) {
            project.d8SetupTaskProvider
        }

    companion object {
        const val EXTENSION_NAME: String = "kotlinD8"
    }
}