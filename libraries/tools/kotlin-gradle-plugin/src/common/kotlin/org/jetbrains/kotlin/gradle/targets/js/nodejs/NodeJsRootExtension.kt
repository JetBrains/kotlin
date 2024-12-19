/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguate
import org.jetbrains.kotlin.gradle.targets.web.nodejs.BaseNodeJsRootExtension

/**
 * Extension for configuring Node.js-related settings at the root level in a Kotlin/JS project.
 *
 * The primary purpose of this extension is to assist in managing tasks, configurations, and settings
 * required for Node.js-based workflows in Kotlin/JS projects, while bridging with the `NodeJsEnvSpec`.
 *
 */
abstract class NodeJsRootExtension internal constructor(
    project: Project,
    nodeJs: () -> NodeJsEnvSpec,
    rootDir: String,
) : BaseNodeJsRootExtension(
    project,
    nodeJs,
    rootDir
), HasPlatformDisambiguate by JsPlatformDisambiguate {
    companion object {
        const val EXTENSION_NAME: String = "kotlinNodeJs"
    }
}
