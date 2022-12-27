package org.jetbrains.kotlin.gradle.util

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.extraProperties

internal fun disableLegacyWarning(project: Project) {
    project.extraProperties.set("kotlin.js.compiler.nowarn", "true")
}