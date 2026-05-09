/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.binaryen

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.targets.js.AbstractSettings
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguator
import org.jetbrains.kotlin.gradle.utils.property

@Deprecated(
    "Use 'org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec' instead. Scheduled for removal in Kotlin 2.6.",
    ReplaceWith(
        "BinaryenEnvSpec",
        "org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec"
    ),
    level = DeprecationLevel.WARNING
)
@OptIn(ExperimentalWasmDsl::class)
open class BinaryenExtension(
    @Transient val project: Project,
    private val binaryenSpec: BinaryenEnvSpec,
) : AbstractSettings<BinaryenEnv>() {
    private val gradleHome = project.gradle.gradleUserHomeDir.also {
        project.logger.kotlinInfo("Storing cached files in $it")
    }

    override val installationDirectory: DirectoryProperty = project.objects.directoryProperty()
        .fileValue(gradleHome.resolve("binaryen"))

    // value not convention because this property can be nullable to not add repository
    override val downloadBaseUrlProperty: org.gradle.api.provider.Property<String> = project.objects.property<String>()
        .value("https://github.com/WebAssembly/binaryen/releases/download")

    override val versionProperty: org.gradle.api.provider.Property<String> = project.objects.property<String>()
        .convention("129")

    override val downloadProperty: org.gradle.api.provider.Property<Boolean> = project.objects.property<Boolean>()
        .convention(true)

    override val commandProperty: org.gradle.api.provider.Property<String> = project.objects.property<String>()
        .convention("wasm-opt")

    val setupTaskProvider: TaskProvider<BinaryenSetupTask>
        get() = project.tasks.withType(BinaryenSetupTask::class.java)
            .named(
                WasmPlatformDisambiguator.extensionName(
                    BinaryenSetupTask.BASE_NAME,
                )
            )

    internal val platform: org.gradle.api.provider.Property<BinaryenPlatform> = project.objects.property<BinaryenPlatform>()

    override fun finalizeConfiguration(): BinaryenEnv {
        return binaryenSpec.env.get()
    }

    companion object : HasPlatformDisambiguator by WasmPlatformDisambiguator {
        val EXTENSION_NAME: String
            get() = extensionName("binaryen")
    }
}
