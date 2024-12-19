/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.web.nodejs.CommonNodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.web.nodejs.NodeJsPluginApplier
import org.jetbrains.kotlin.gradle.utils.castIsolatedKotlinPluginClassLoaderAware

/**
 * Abstract base class for applying Node.js-specific configurations and tasks to a Gradle project.
 * This class integrates Node.js environment specifications and setup tasks into a Gradle project.
 * It extends the functionality provided by `CommonNodeJsPlugin`.
 *
 * The functionality includes:
 * - Applying the Node.js plugin to a target project.
 * - Integrating the `NodeJsEnvSpec` extension for managing Node.js environment configurations.
 * - Automatically setting up tasks like downloading and installing a local Node.js/npm version.
 */
abstract class NodeJsPlugin internal constructor() : CommonNodeJsPlugin {
    override fun apply(target: Project) {
        NodeJsPluginApplier(
            platformDisambiguate = JsPlatformDisambiguate,
            nodeJsEnvSpecKlass = NodeJsEnvSpec::class,
            nodeJsEnvSpecName = NodeJsEnvSpec.EXTENSION_NAME,
            nodeJsRootApply = { NodeJsRootPlugin.apply(it) }
        ).apply(target)
    }

    companion object {
        fun apply(project: Project): NodeJsEnvSpec {
            project.plugins.apply(NodeJsPlugin::class.java)
            return project.extensions.getByName(NodeJsEnvSpec.EXTENSION_NAME) as NodeJsEnvSpec
        }

        val Project.kotlinNodeJsEnvSpec: NodeJsEnvSpec
            get() = extensions.getByName(NodeJsEnvSpec.EXTENSION_NAME).castIsolatedKotlinPluginClassLoaderAware()
    }
}
