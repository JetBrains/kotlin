/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KLIB_COMMONIZER_CLASSPATH_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import java.io.File

internal class KotlinNativeCommonizerToolRunner(project: Project) : KotlinToolRunner(project) {
    override val displayName get() = "Kotlin/Native KLIB commonizer"

    override val mainClass: String get() = "org.jetbrains.kotlin.commonizer.cli.CommonizerCLI"

    override val classpath by lazy {
        try {
            project.configurations.getByName(KLIB_COMMONIZER_CLASSPATH_CONFIGURATION_NAME).resolve() as Set<File>
        } catch (e: Exception) {
            project.logger.error(
                "Could not resolve KLIB commonizer classpath. Check if Kotlin Gradle plugin repository is configured in $project."
            )
            throw e
        }
    }

    override val isolatedClassLoaderCacheKey get() = project.getKotlinPluginVersion()

    override val defaultMaxHeapSize: String get() = "4G"

    override val mustRunViaExec get() = true // because it's not enough the standard Gradle wrapper's heap size

    override fun getCustomJvmArgs() = PropertiesProvider(project).commonizerJvmArgs?.split("\\s+".toRegex()).orEmpty()
}
