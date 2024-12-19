/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguate
import org.jetbrains.kotlin.gradle.targets.web.nodejs.AbstractNodeJsRootExtension

open class NodeJsRootExtension(
    project: Project,
    nodeJs: () -> NodeJsEnvSpec,
    rootDir: String,
) : AbstractNodeJsRootExtension(
    project,
    nodeJs,
    rootDir
), HasPlatformDisambiguate by JsPlatformDisambiguate {
    companion object {
        const val EXTENSION_NAME: String = "kotlinNodeJs"
    }
}
