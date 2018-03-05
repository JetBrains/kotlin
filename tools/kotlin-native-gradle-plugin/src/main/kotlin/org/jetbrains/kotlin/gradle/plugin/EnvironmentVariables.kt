package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KonanPlugin.ProjectProperty
import java.io.File

/**
 *  The plugin allows an IDE to specify some building parameters. These parameters
 *  are passed to the plugin via environment variables. Two variables are supported:
 *      - CONFIGURATION_BUILD_DIR - an absolute path to a destination directory for all compilation tasks.
 *                                  The IDE should take care about specifying different directories
 *                                  for different targets. This setting has less priority than
 *                                  an explicitly specified destination directory in the build script.
 *      - DEBUGGING_SYMBOLS - If YES, the debug support will be enabled for all artifacts. This option has less
 *                            priority than explicitly specified enableDebug option in the build script and
 *                            enableDebug project property.
 *
 *  Support for environment variables should be explicitly enabled by setting a project property:
 *      konan.useEnvironmentVariables = true.
 */

internal interface EnvironmentVariables {
    val configurationBuildDir: File?
    val debuggingSymbols: Boolean
}

internal class EnvironmentVariablesUnused: EnvironmentVariables {
    override val configurationBuildDir: File?
        get() = null

    override val debuggingSymbols: Boolean
        get() = false
}

internal class EnvironmentVariablesImpl:  EnvironmentVariables {
    override val configurationBuildDir: File?
        get() = System.getenv("CONFIGURATION_BUILD_DIR")?.let {
            File(it).apply { check(isAbsolute) { "A path passed using CONFIGURATION_BUILD_DIR should be absolute" } }
        }

    override val debuggingSymbols: Boolean
        get() = System.getenv("DEBUGGING_SYMBOLS")?.toUpperCase() == "YES"
}

internal val Project.useEnvironmentVariables: Boolean
    get() = findProperty(ProjectProperty.KONAN_USE_ENVIRONMENT_VARIABLES)?.toString()?.toBoolean() ?: false

internal val Project.environmentVariables: EnvironmentVariables
    get() = if (useEnvironmentVariables) {
        EnvironmentVariablesImpl()
    } else {
        EnvironmentVariablesUnused()
    }
