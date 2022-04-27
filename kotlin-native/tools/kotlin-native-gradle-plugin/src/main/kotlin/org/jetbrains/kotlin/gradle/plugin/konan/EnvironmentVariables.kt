/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle.plugin.konan

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.konan.KonanPlugin.ProjectProperty
import java.io.File
import java.util.*

/**
 *  The plugin allows an IDE to specify some building parameters. These parameters
 *  are passed to the plugin via environment variables. Two variables are supported:
 *      - CONFIGURATION_BUILD_DIR    - A path to a destination directory for all compilation tasks.
 *                                     The IDE should take care about specifying different directories
 *                                     for different targets. This setting has less priority than
 *                                     an explicitly specified destination directory in the build script.
 *
 *      - DEBUGGING_SYMBOLS          - If YES, the debug support will be enabled for all artifacts. This option has less
 *                                     priority than explicitly specified enableDebug option in the build script and
 *                                     enableDebug project property.
 *
 *      - KONAN_ENABLE_OPTIMIZATIONS - If YES, optimizations will be enabled for all artifacts by default. This option
 *                                     has less priority than explicitly specified enableOptimizations option in the
 *                                     build script.
 *
 *  Support for environment variables should be explicitly enabled by setting a project property:
 *      konan.useEnvironmentVariables = true.
 */

internal interface EnvironmentVariables {
    val configurationBuildDir: File?
    val debuggingSymbols: Boolean
    val enableOptimizations: Boolean
}

internal class EnvironmentVariablesUnused: EnvironmentVariables {
    override val configurationBuildDir: File?
        get() = null

    override val debuggingSymbols: Boolean
        get() = false

    override val enableOptimizations: Boolean
        get() = false
}

internal class EnvironmentVariablesImpl(val project: Project):  EnvironmentVariables {
    override val configurationBuildDir: File?
        get() = System.getenv("CONFIGURATION_BUILD_DIR")?.let {
            project.file(it)
        }

    override val debuggingSymbols: Boolean
        get() = System.getenv("DEBUGGING_SYMBOLS")?.uppercase(Locale.getDefault()) == "YES"

    override val enableOptimizations: Boolean
        get() = System.getenv("KONAN_ENABLE_OPTIMIZATIONS")?.uppercase(Locale.getDefault()) == "YES"
}

/**
 * Due to https://github.com/gradle/gradle/issues/3468 we cannot use environment
 * variables in Java 9. Until Gradle API for environment variables is provided
 * we use project properties instead of them. TODO: Return to using env vars when the issue is fixed.
 */
internal class EnvironmentVariablesFromProperties(val project: Project): EnvironmentVariables {
    override val configurationBuildDir: File?
        get() = project.findProperty(ProjectProperty.KONAN_CONFIGURATION_BUILD_DIR)?.let {
            project.file(it)
        }

    override val debuggingSymbols: Boolean
        get() = project.findProperty(ProjectProperty.KONAN_DEBUGGING_SYMBOLS)?.toString()?.uppercase(Locale.getDefault()).let {
            it == "YES" || it == "TRUE"
        }

    override val enableOptimizations: Boolean
        get() = project.findProperty(ProjectProperty.KONAN_OPTIMIZATIONS_ENABLE)?.toString()?.uppercase(Locale.getDefault()).let {
            it == "YES" || it == "TRUE"
        }
}

internal val Project.useEnvironmentVariables: Boolean
    get() = findProperty(ProjectProperty.KONAN_USE_ENVIRONMENT_VARIABLES)?.toString()?.toBoolean() ?: false

/*
 TODO: Return to using env vars when the issue is fixed.
 Take into account the useEnvironmentVariables property (and may be rename it) in the following way:

 if (useEnvironmentVariables) {
     EnvironmentVariablesImpl(project)
 } else {
     EnvironmentVariablesUnused()
 }
*/
internal val Project.environmentVariables: EnvironmentVariables
    get() = EnvironmentVariablesFromProperties(project)
