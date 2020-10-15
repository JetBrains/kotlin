/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.resolver

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskCollection
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.whenEvaluated
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependencies
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinProjectNpmResolution
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import kotlin.reflect.KClass

/**
 * See [KotlinNpmResolutionManager] for details about resolution process.
 */
internal class KotlinProjectNpmResolver(
    val project: Project,
    val resolver: KotlinRootNpmResolver
) {
    override fun toString(): String = "ProjectNpmResolver($project)"

    private val byCompilation = mutableMapOf<KotlinJsCompilation, KotlinCompilationNpmResolver>()

    operator fun get(compilation: KotlinJsCompilation): KotlinCompilationNpmResolver {
        check(compilation.target.project == project)
        return byCompilation[compilation] ?: error("$compilation was not registered in $this")
    }

    private var closed = false

    val compilationResolvers: Collection<KotlinCompilationNpmResolver>
        get() = byCompilation.values

    init {
        addContainerListeners()

        project.whenEvaluated {
            val nodeJs = resolver.nodeJs
            project.tasks.implementing(RequiresNpmDependencies::class)
                .configureEach { task ->
                    if (task.enabled) {
                        task as RequiresNpmDependencies
                        // KotlinJsTest delegates npm dependencies to testFramework,
                        // which can be defined after this configure action
                        val packageJsonTaskHolder = get(task.compilation).packageJsonTaskHolder
                        if (task !is KotlinJsTest) {
                            nodeJs.taskRequirements.addTaskRequirements(task)
                        }
                        task.dependsOn(packageJsonTaskHolder)
                        task.dependsOn(nodeJs.npmInstallTaskProvider)
                    }
                }
        }
    }

    private fun addContainerListeners() {
        val kotlin = project.kotlinExtensionOrNull
            ?: error("NpmResolverPlugin should be applied after kotlin plugin")

        when (kotlin) {
            is KotlinSingleTargetExtension -> addTargetListeners(kotlin.target)
            is KotlinMultiplatformExtension -> kotlin.targets.all {
                addTargetListeners(it)
            }
            else -> error("Unsupported kotlin model: $kotlin")
        }
    }

    private fun addTargetListeners(target: KotlinTarget) {
        check(!closed) { resolver.alreadyResolvedMessage("add target $target") }

        if (target.platformType == KotlinPlatformType.js) {
            target.compilations.all { compilation ->
                if (compilation is KotlinJsCompilation) {
                    // compilation may be KotlinWithJavaTarget for old Kotlin2JsPlugin
                    addCompilation(compilation)
                }
            }

            // Hack for mixed mode, when target is JS and contain JS-IR
            if (target is KotlinJsTarget) {
                target.irTarget?.compilations?.all { compilation ->
                    if (compilation is KotlinJsCompilation) {
                        addCompilation(compilation)
                    }
                }
            }
        }
    }

    @Synchronized
    private fun addCompilation(compilation: KotlinJsCompilation) {
        check(!closed) { resolver.alreadyResolvedMessage("add compilation $compilation") }

        byCompilation[compilation] = KotlinCompilationNpmResolver(this, compilation)
    }

    fun close(): KotlinProjectNpmResolution {
        check(!closed)
        closed = true

        return KotlinProjectNpmResolution(
            project,
            byCompilation.values.mapNotNull { it.close() },
            resolver.nodeJs.taskRequirements.byTask
        )
    }
}


/**
 * Filters a [TaskCollection] by type that is not a subtype of [Task] (for use with interfaces)
 *
 * TODO properly express within the type system? The result should be a TaskCollection<T & R>
 */
internal fun <T : Task, R : Any> TaskCollection<T>.implementing(kclass: KClass<R>): TaskCollection<T> =
    @Suppress("UNCHECKED_CAST")
    withType(kclass.java as Class<T>)
