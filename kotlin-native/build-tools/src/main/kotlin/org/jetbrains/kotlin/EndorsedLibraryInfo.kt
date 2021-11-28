package org.jetbrains.kotlin

import org.gradle.api.Project

data class EndorsedLibraryInfo(val project: Project, val name: String) {

    val projectName: String
        get() = project.name

    val taskName: String by lazy {
        projectName.split('.').joinToString(separator = "") { it.capitalize() }
    }
}