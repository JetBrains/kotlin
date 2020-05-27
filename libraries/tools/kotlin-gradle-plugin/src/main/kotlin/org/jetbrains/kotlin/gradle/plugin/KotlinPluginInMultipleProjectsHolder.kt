/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension

internal class KotlinPluginInMultipleProjectsHolder(
    private val differentVersionsInDifferentProject: Boolean
) {
    private var loadedInProjectPath: String? = null

    fun addProject(
        project: Project,
        kotlinPluginVersion: String? = null,
        onRegister: () -> Unit = {}
    ) {
        require(differentVersionsInDifferentProject == (kotlinPluginVersion != null))

        val projectPath = project.path

        val loadedInProjectsPropertyName = getPropertyName(kotlinPluginVersion)

        if (!differentVersionsInDifferentProject || loadedInProjectPath == null) {
            loadedInProjectPath = projectPath

            val ext = project.rootProject.extensions.getByType(ExtraPropertiesExtension::class.java)

            if (!ext.has(loadedInProjectsPropertyName)) {
                ext.set(loadedInProjectsPropertyName, projectPath)
                onRegister()
            } else {
                ext.set(loadedInProjectsPropertyName, (ext.get(loadedInProjectsPropertyName) as String) + ";" + loadedInProjectPath)
            }
        }
    }

    fun isInMultipleProjects(
        project: Project,
        kotlinPluginVersion: String? = null
    ): Boolean {
        require(differentVersionsInDifferentProject == (kotlinPluginVersion != null))
        return getAffectedProjects(project, kotlinPluginVersion)?.let {
            it.size > 1
        } ?: false
    }

    fun getAffectedProjects(
        project: Project,
        kotlinPluginVersion: String? = null
    ): List<String>? {
        require(differentVersionsInDifferentProject == (kotlinPluginVersion != null))

        val ext = getExt(project)

        val propertyName = getPropertyName(kotlinPluginVersion)

        if (!ext.has(propertyName)) {
            return null
        }

        return (ext.get(propertyName) as String).split(";")
    }

    private fun getExt(project: Project): ExtraPropertiesExtension {
        return project.rootProject.extensions.getByType(ExtraPropertiesExtension::class.java)
    }

    private fun getPropertyName(version: String?): String {
        return listOfNotNull(
            "kotlin",
            "plugin",
            "loaded",
            "in",
            "projects",
            version
        )
            .joinToString(".")
    }
}

const val MULTIPLE_KOTLIN_PLUGINS_LOADED_WARNING: String =
    "The Kotlin Gradle plugin was loaded multiple times in different subprojects, which is not supported and may break the build. \n" +

            "This might happen in subprojects that apply the Kotlin plugins with the Gradle 'plugins { ... }' DSL if they specify " +
            "explicit versions, even if the versions are equal.\n" +

            "Please add the Kotlin plugin to the common parent project or the root project, then remove the versions in the subprojects.\n" +

            "If the parent project does not need the plugin, add 'apply false' to the plugin line.\n" +

            "See: https://docs.gradle.org/current/userguide/plugins.html#sec:subprojects_plugins_dsl"

const val MULTIPLE_KOTLIN_PLUGINS_SPECIFIC_PROJECTS_WARNING: String =
    "The Kotlin plugin was loaded in the following projects: "

const val MULTIPLE_KOTLIN_PLUGINS_SPECIFIC_PROJECTS_INFO: String = "The full list of projects that loaded the Kotlin plugin is: "