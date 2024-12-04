/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleJavaTargetExtension
import org.jetbrains.kotlin.gradle.dsl.topLevelExtension
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool

abstract class KotlinCompilationProcessor<out T : AbstractKotlinCompileTool<*>> internal constructor(
    internal val compilationInfo: KotlinCompilationInfo
) {

    abstract val kotlinTask: TaskProvider<out T>
    abstract fun run()

    protected val project: Project
        get() = compilationInfo.project

    protected val defaultKotlinDestinationDir: Provider<Directory>
        get() {
            val kotlinExt = project.topLevelExtension
            val targetSubDirectory =
                if (kotlinExt is KotlinSingleJavaTargetExtension)
                    "" // In single-target projects, don't add the target name part to this path
                else
                    compilationInfo.targetDisambiguationClassifier?.let { "$it/" }.orEmpty()
            return project.layout.buildDirectory.dir("classes/kotlin/$targetSubDirectory${compilationInfo.compilationName}")
        }
}
