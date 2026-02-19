/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.npm.KotlinNpmResolutionManager
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmExtension
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.web.nodejs.CommonNodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.web.nodejs.NodeJsRootPluginApplier
import org.jetbrains.kotlin.gradle.utils.castIsolatedKotlinPluginClassLoaderAware

/**
 * Abstract base class for configuring Node.js-related setups at the root project level in a Gradle build.
 *
 * NodeJsRootPlugin automates the integration of Node.js runtime, npm, and other related tools
 * for Kotlin/JS projects in a multi-project build environment. It applies appropriate plugins,
 * initializes extensions, and configures tasks required to enable JavaScript and Node.js
 * interoperability in projects.
 *
 * This plugin is applied to the root project, ensuring consistent configuration for
 * Node.js tooling and dependency management across all subprojects that rely on Kotlin/JS.
 *
 * Key functionalities include:
 * - Applying the core Node.js setup plugin to the target.
 * - Configuring npm and Yarn extensions.
 * - Managing root-level configurations for Node.js and dependency resolution.
 * - Setting up shared resources such as lock files and task groups.
 */
abstract class NodeJsRootPlugin internal constructor() : CommonNodeJsRootPlugin {

    override fun apply(target: Project) {
        NodeJsRootPluginApplier(
            platformDisambiguate = JsPlatformDisambiguator,
            nodeJsRootKlass = NodeJsRootExtension::class,
            nodeJsRootName = NodeJsRootExtension.EXTENSION_NAME,
            npmKlass = NpmExtension::class,
            npmName = NpmExtension.EXTENSION_NAME,
            rootDirectoryName = JsPlatformDisambiguator.jsPlatform,
            lockFileDirectory = { it.dir(LockCopyTask.KOTLIN_JS_STORE) },
            singleNodeJsPluginApply = { NodeJsPlugin.apply(it) },
            yarnPlugin = YarnPlugin::class,
            platformType = KotlinPlatformType.js,
        ).apply(target)
    }

    companion object {
        const val TASKS_GROUP_NAME: String = "nodeJs"

        fun apply(rootProject: Project): NodeJsRootExtension {
            check(rootProject == rootProject.rootProject)
            rootProject.plugins.apply(NodeJsRootPlugin::class.java)
            return rootProject.extensions.getByName(NodeJsRootExtension.EXTENSION_NAME) as NodeJsRootExtension
        }

        val Project.kotlinNodeJsRootExtension: NodeJsRootExtension
            get() = extensions.getByName(NodeJsRootExtension.EXTENSION_NAME).castIsolatedKotlinPluginClassLoaderAware()

        val Project.kotlinNpmResolutionManager: Provider<KotlinNpmResolutionManager>
            get() {
                return project.gradle.sharedServices.registerIfAbsent(
                    KotlinNpmResolutionManager::class.java.name,
                    KotlinNpmResolutionManager::class.java
                ) {
                    error("Must be already registered")
                }
            }
    }
}
