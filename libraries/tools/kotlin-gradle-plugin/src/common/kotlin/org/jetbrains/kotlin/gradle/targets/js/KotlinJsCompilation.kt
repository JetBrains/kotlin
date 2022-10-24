/*
* Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
* Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
*/

@file:Suppress("PackageDirectoryMismatch")

// Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import groovy.lang.Closure
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.HasCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationWithResources
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsSubTargetContainerDsl
import org.jetbrains.kotlin.gradle.targets.js.ir.JsBinary
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsBinaryContainer
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJson
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import javax.inject.Inject

abstract class KotlinJsCompilation @Inject internal constructor(
    compilationDetails: JsCompilationDetails
) : AbstractKotlinCompilationToRunnableFiles<KotlinJsOptions>(compilationDetails),
    KotlinCompilationWithResources<KotlinJsOptions> {

    final override val target: KotlinTarget get() = super.target

    @Suppress("UNCHECKED_CAST")
    final override val compilerOptions: HasCompilerOptions<KotlinJsCompilerOptions>
        get() = super.compilerOptions as HasCompilerOptions<KotlinJsCompilerOptions>

    internal val binaries: KotlinJsBinaryContainer =
        target.project.objects.newInstance(
            KotlinJsBinaryContainer::class.java,
            target,
            target.project.objects.domainObjectSet(JsBinary::class.java)
        )

    var outputModuleName: String? = null
        set(value) {
            (target as KotlinJsSubTargetContainerDsl).apply {
                check(!isBrowserConfigured && !isNodejsConfigured) {
                    "Please set outputModuleName for compilation before initialize browser() or nodejs() on target"
                }
            }

            field = value
        }

    override val processResourcesTaskName: String
        get() = disambiguateName("processResources")

    @Suppress("DEPRECATION")
    @Deprecated("Accessing task instance directly is deprecated", replaceWith = ReplaceWith("compileTaskProvider"))
    override val compileKotlinTask: Kotlin2JsCompile
        get() = super.compileKotlinTask as Kotlin2JsCompile

    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    @Deprecated("Replaced with compileTaskProvider", replaceWith = ReplaceWith("compileTaskProvider"))
    override val compileKotlinTaskProvider: TaskProvider<out Kotlin2JsCompile>
        get() = super.compileKotlinTaskProvider as TaskProvider<out Kotlin2JsCompile>

    @Suppress("UNCHECKED_CAST")
    override val compileTaskProvider: TaskProvider<Kotlin2JsCompile>
        get() = super.compileTaskProvider as TaskProvider<Kotlin2JsCompile>

    internal val packageJsonHandlers = mutableListOf<PackageJson.() -> Unit>()

    fun packageJson(handler: PackageJson.() -> Unit) {
        packageJsonHandlers.add(handler)
    }

    fun packageJson(handler: Closure<*>) {
        packageJson {
            project.configure(this, handler)
        }
    }
}