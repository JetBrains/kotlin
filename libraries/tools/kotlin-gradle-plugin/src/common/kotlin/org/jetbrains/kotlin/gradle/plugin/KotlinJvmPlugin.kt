/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.*
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.tasks.*

const val KOTLIN_DSL_NAME = "kotlin"

@Deprecated("Should be removed with 'platform.js' plugin removal")
const val KOTLIN_JS_DSL_NAME = "kotlin2js"
const val KOTLIN_OPTIONS_DSL_NAME = "kotlinOptions"

internal open class KotlinJvmPlugin(
    registry: ToolingModelBuilderRegistry
) : AbstractKotlinPlugin(KotlinTasksProvider(), registry) {

    internal companion object {
        private const val targetName = "" // use empty suffix for the task names

        internal fun Project.configureCompilerOptionsForTarget(
            extensionCompilerOptions: KotlinJvmCompilerOptions,
            targetCompilerOptions: KotlinJvmCompilerOptions
        ) {
            extensionCompilerOptions.moduleName.convention(baseModuleName())
            DefaultKotlinJavaToolchain.wireJvmTargetToToolchain(
                extensionCompilerOptions,
                project
            )
            KotlinJvmCompilerOptionsHelper.syncOptionsAsConvention(
                from = extensionCompilerOptions,
                into = targetCompilerOptions
            )
        }
    }

    override fun buildSourceSetProcessor(project: Project, compilation: KotlinCompilation<*>) =
        Kotlin2JvmSourceSetProcessor(tasksProvider, KotlinCompilationInfo(compilation))

    override fun apply(project: Project) {
        val target = project.objects.KotlinWithJavaTargetForJvm(project, targetName)
        val kotlinExtension = project.kotlinExtension as KotlinJvmProjectExtension
        kotlinExtension.targetFuture.complete(target)

        super.apply(project)

        project.configureCompilerOptionsForTarget(
            kotlinExtension.compilerOptions,
            target.compilerOptions
        )
    }

    override fun configureClassInspectionForIC(project: Project) {
        // For new IC this task is not needed
        if (!project.kotlinPropertiesProvider.useClasspathSnapshot) {
            super.configureClassInspectionForIC(project)
        }
    }
}
