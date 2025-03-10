/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.binaryen

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.targets.js.AbstractSettings
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnv
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenPlatform
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenSetupTask
import org.jetbrains.kotlin.gradle.utils.property

@Deprecated(
    "Use 'org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec' instead",
    ReplaceWith(
        "BinaryenEnvSpec",
        "org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec"
    )
)
@OptIn(ExperimentalWasmDsl::class)
open class BinaryenExtension(
    @Transient val rootProject: Project,
    private val binaryenSpec: BinaryenEnvSpec,
) : AbstractSettings<BinaryenEnv>() {

    private val gradleHome = rootProject.gradle.gradleUserHomeDir.also {
        rootProject.logger.kotlinInfo("Storing cached files in $it")
    }

    override val installationDirectory: DirectoryProperty = rootProject.objects.directoryProperty()
        .fileValue(gradleHome.resolve("binaryen"))

    // value not convention because this property can be nullable to not add repository
    override val downloadBaseUrlProperty: org.gradle.api.provider.Property<String> = rootProject.objects.property<String>()
        .value("https://github.com/WebAssembly/binaryen/releases/download")

    override val versionProperty: org.gradle.api.provider.Property<String> = rootProject.objects.property<String>()
        .convention("122")

    override val downloadProperty: org.gradle.api.provider.Property<Boolean> = rootProject.objects.property<Boolean>()
        .convention(true)

    override val commandProperty: org.gradle.api.provider.Property<String> = rootProject.objects.property<String>()
        .convention("wasm-opt")

    val setupTaskProvider: TaskProvider<BinaryenSetupTask>
        get() = rootProject.tasks.withType(BinaryenSetupTask::class.java).named(BinaryenSetupTask.NAME)

    internal val platform: org.gradle.api.provider.Property<BinaryenPlatform> = rootProject.objects.property<BinaryenPlatform>()

    override fun finalizeConfiguration(): BinaryenEnv {
        return binaryenSpec.env.get()
    }

    companion object {
        const val EXTENSION_NAME: String = org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenExtension.EXTENSION_NAME
    }
}
