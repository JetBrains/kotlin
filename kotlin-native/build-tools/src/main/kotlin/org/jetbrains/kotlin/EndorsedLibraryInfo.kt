package org.jetbrains.kotlin

import org.gradle.api.Project

@OptIn(ExperimentalStdlibApi::class)
data class EndorsedLibraryInfo(val project: Project, val name: String) {

    val projectName: String
        get() = project.name

    val taskName: String by lazy {
        projectName.split('.').joinToString(separator = "") { name -> name.replaceFirstChar { it.uppercase() } }
    }
}