/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.nodejs

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.web.nodejs.BaseNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.web.nodejs.NpmToolingEnv
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinToolingSetupTask
import org.jetbrains.kotlin.gradle.utils.property

/**
 * Extension for configuring Node.js-specific settings and tasks in a Kotlin/JS project,
 * specifically tailored towards supporting WebAssembly (Wasm) runtime via Node.js.
 */
abstract class WasmNodeJsRootExtension internal constructor(
    project: Project,
    nodeJs: () -> NodeJsEnvSpec,
    rootDir: String,
) : BaseNodeJsRootExtension(
    project,
    nodeJs,
    rootDir
), HasPlatformDisambiguator by WasmPlatformDisambiguator {

    val npmTooling: Property<NpmToolingEnv> = project.objects.property()

    val toolingInstallTaskProvider: TaskProvider<out KotlinToolingSetupTask>
        get() = project.tasks.withType(KotlinToolingSetupTask::class.java)
            .named(extensionName(KotlinToolingSetupTask.BASE_NAME))

    companion object : HasPlatformDisambiguator by WasmPlatformDisambiguator {
        val EXTENSION_NAME: String
            get() = extensionName("nodeJs")
    }
}