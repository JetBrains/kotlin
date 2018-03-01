package org.jetbrains.kotlin.gradle.plugin

import java.io.File

object EnvironmentVariables {
    val configurationBuildDir: File?
        get() = System.getenv("CONFIGURATION_BUILD_DIR")?.let {
            File(it).apply {check(isAbsolute) { "A path passed using CONFIGURATION_BUILD_DIR should be absolute" } }
        }

    val debuggingSymbols: Boolean
        get() = System.getenv("DEBUGGING_SYMBOLS")?.toUpperCase() == "YES"
}