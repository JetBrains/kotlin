/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.web.npm.CommonNpmResolverPlugin
import org.jetbrains.kotlin.gradle.targets.web.npm.NpmResolverPluginApplier

/**
 * A plugin for configuring and resolving NPM dependencies in a Gradle project.
 *
 * This plugin integrates Node.js and NPM dependency management into the Gradle build process.
 * It ensures configuration of the required Node.js environment and applies project-specific
 * Node.js configurations necessary for resolving and using NPM dependencies.
 *
 * This plugin is suitable for projects using NPM dependencies, and its application ensures consistency
 * and correctness in dealing with those dependencies.
 */
class NpmResolverPlugin : CommonNpmResolverPlugin {
    override fun apply(project: Project) {
        NpmResolverPluginApplier(
            { NodeJsRootPlugin.apply(project.rootProject) },
            { NodeJsPlugin.apply(project) },
        ).apply(project)
    }

    companion object {
        fun apply(project: Project) {
            project.plugins.apply(NpmResolverPlugin::class.java)
        }
    }
}