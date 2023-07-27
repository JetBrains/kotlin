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
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import org.jetbrains.kotlin.gradle.targets.js.npm.KotlinNpmResolutionManager
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinProjectNpmResolution
import java.io.Serializable
import kotlin.reflect.KClass

/**
 * See [KotlinNpmResolutionManager] for details about resolution process.
 */
class KotlinProjectNpmResolver(
    project: Project,
    var resolver: KotlinRootNpmResolver
) : Serializable {
    val projectPath by lazy { project.path }

    private val byCompilation = mutableMapOf<String, KotlinCompilationNpmResolver>()

    operator fun get(compilation: KotlinJsCompilation): KotlinCompilationNpmResolver {
        return byCompilation[compilation.disambiguatedName] ?: error("$compilation was not registered in $this")
    }

    operator fun get(compilationName: String): KotlinCompilationNpmResolver {
        return byCompilation[compilationName] ?: error("$compilationName was not registered in $this")
    }

    private var resolution: KotlinProjectNpmResolution? = null

    val compilationResolvers: Collection<KotlinCompilationNpmResolver>
        get() = byCompilation.values

    init {
        project.addContainerListeners()
    }

    private fun Project.addContainerListeners() {
        val kotlin = kotlinExtensionOrNull
            ?: error("NpmResolverPlugin should be applied after kotlin plugin")

        when (kotlin) {
            is KotlinSingleTargetExtension<*> -> addTargetListeners(kotlin.target)
            is KotlinMultiplatformExtension -> kotlin.targets.all {
                addTargetListeners(it)
            }
            else -> error("Unsupported kotlin model: $kotlin")
        }
    }

    private fun addTargetListeners(target: KotlinTarget) {
        check(resolution == null) { resolver.alreadyResolvedMessage("add target $target") }

        if (target.platformType == KotlinPlatformType.js ||
            target.platformType == KotlinPlatformType.wasm
        ) {
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
        check(resolution == null) { resolver.alreadyResolvedMessage("add compilation $compilation") }

        byCompilation[compilation.disambiguatedName] =
            KotlinCompilationNpmResolver(
                this,
                compilation
            )
    }

    fun close(): KotlinProjectNpmResolution {
        return resolution ?: KotlinProjectNpmResolution(
            byCompilation
                .map { (key, value) ->
                    value.close()?.let { key to it }
                }
                .filterNotNull()
                .toMap(),
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
