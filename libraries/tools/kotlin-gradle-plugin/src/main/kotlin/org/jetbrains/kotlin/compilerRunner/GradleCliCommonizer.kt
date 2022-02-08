/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.compilerRunner

import org.gradle.api.Project
import org.jetbrains.kotlin.commonizer.CliCommonizer
import org.jetbrains.kotlin.gradle.internal.KOTLIN_MODULE_GROUP
import org.jetbrains.kotlin.gradle.plugin.KLIB_COMMONIZER_CLASSPATH_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

private const val KOTLIN_KLIB_COMMONIZER_EMBEDDABLE = "kotlin-klib-commonizer-embeddable"

/**
 * Creates an instance of [CliCommonizer] that is backed by [KotlinNativeCommonizerToolRunner] to adhere to user defined settings
 * when executing the commonizer (like jvm arguments, running in separate process, etc)
 */
internal fun GradleCliCommonizer(project: Project): CliCommonizer {
    return GradleCliCommonizer(
        KotlinNativeCommonizerToolRunner(project)
    )
}

internal fun GradleCliCommonizer(commonizerToolRunner: KotlinNativeCommonizerToolRunner): CliCommonizer {
    return CliCommonizer(CliCommonizer.Executor { arguments ->
        commonizerToolRunner.run(arguments)
    })
}

internal fun Project.registerCommonizerClasspathConfigurationIfNecessary() {
    if (configurations.findByName(KLIB_COMMONIZER_CLASSPATH_CONFIGURATION_NAME) == null) {
        project.configurations.create(KLIB_COMMONIZER_CLASSPATH_CONFIGURATION_NAME).defaultDependencies {
            it.add(project.dependencies.create("$KOTLIN_MODULE_GROUP:$KOTLIN_KLIB_COMMONIZER_EMBEDDABLE:${getKotlinPluginVersion()}"))
        }
    }
}
