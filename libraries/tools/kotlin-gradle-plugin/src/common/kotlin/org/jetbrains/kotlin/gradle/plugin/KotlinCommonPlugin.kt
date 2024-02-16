/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinWithJavaTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider
import org.jetbrains.kotlin.gradle.utils.configureExperimentalTryNext

internal open class KotlinCommonPlugin(
    registry: ToolingModelBuilderRegistry
) : AbstractKotlinPlugin(KotlinTasksProvider(), registry) {

    companion object {
        private const val targetName = "common"
    }

    override fun buildSourceSetProcessor(
        project: Project,
        compilation: KotlinCompilation<*>
    ): KotlinSourceSetProcessor<*> =
        KotlinCommonSourceSetProcessor(KotlinCompilationInfo(compilation), tasksProvider)

    override fun apply(project: Project) {
        @Suppress("UNCHECKED_CAST", "TYPEALIAS_EXPANSION_DEPRECATION")
        val target = project.objects.newInstance(
            KotlinWithJavaTarget::class.java,
            project,
            KotlinPlatformType.common,
            targetName,
            {
                object : DeprecatedHasCompilerOptions<KotlinMultiplatformCommonCompilerOptions> {
                    override val options: KotlinMultiplatformCommonCompilerOptions = project.objects
                        .newInstance(KotlinMultiplatformCommonCompilerOptionsDefault::class.java)
                        .configureExperimentalTryNext(project)
                }
            },
            { compilerOptions: KotlinMultiplatformCommonCompilerOptions ->
                object : KotlinMultiplatformCommonOptions {
                    override val options: KotlinMultiplatformCommonCompilerOptions
                        get() = compilerOptions
                }
            }
        ) as KotlinWithJavaTarget<KotlinMultiplatformCommonOptions, KotlinMultiplatformCommonCompilerOptions>
        (project.kotlinExtension as KotlinCommonProjectExtension).targetFuture.complete(target)

        super.apply(project)
    }
}