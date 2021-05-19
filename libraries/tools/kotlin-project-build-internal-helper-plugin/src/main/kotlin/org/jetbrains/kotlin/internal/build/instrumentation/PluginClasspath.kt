/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.internal.build.instrumentation

import org.gradle.api.Project
import java.io.File

internal object PluginClasspath {
    private var pluginClasspath: List<File>? = null

    fun get(project: Project): List<File> {
        if (pluginClasspath != null) return pluginClasspath!!

        val buildscriptClasspath = project.rootProject.buildscript.configurations.getByName("classpath")
        val pluginDependency = buildscriptClasspath.resolvedConfiguration.firstLevelModuleDependencies.first {
            it.moduleGroup == "org.jetbrains.kotlin" && it.moduleName == "kotlin-project-build-internal-helper-plugin"
        }
        val classpath = pluginDependency.allModuleArtifacts.map { it.file }
        pluginClasspath = classpath
        return classpath
    }
}