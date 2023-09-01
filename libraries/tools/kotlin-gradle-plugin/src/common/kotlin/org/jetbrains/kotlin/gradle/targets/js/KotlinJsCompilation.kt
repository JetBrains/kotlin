/*
* Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
* Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
*/

@file:Suppress("PackageDirectoryMismatch")

// Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJsOptions
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationImpl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsSubTargetContainerDsl
import org.jetbrains.kotlin.gradle.targets.js.ir.JsBinary
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsBinaryContainer
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJson
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import javax.inject.Inject

open class KotlinJsCompilation @Inject internal constructor(
    compilation: KotlinCompilationImpl
) : AbstractKotlinCompilationToRunnableFiles<KotlinJsOptions>(compilation) {

    @Suppress("UNCHECKED_CAST")
    final override val compilerOptions: HasCompilerOptions<KotlinJsCompilerOptions>
        get() = compilation.compilerOptions as HasCompilerOptions<KotlinJsCompilerOptions>

    fun compilerOptions(configure: KotlinJsCompilerOptions.() -> Unit) {
        compilerOptions.configure(configure)
    }

    fun compilerOptions(configure: Action<KotlinJsCompilerOptions>) {
        configure.execute(compilerOptions.options)
    }

    val binaries: KotlinJsBinaryContainer =
        compilation.target.project.objects.newInstance(
            KotlinJsBinaryContainer::class.java,
            compilation.target,
            compilation.target.project.objects.domainObjectSet(JsBinary::class.java)
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

    @Deprecated("Use compilationName instead", ReplaceWith("compilationName"))
    val compilationPurpose: String get() = compilationName

    override val processResourcesTaskName: String
        get() = disambiguateName("processResources")

    val npmAggregatedConfigurationName
        get() = compilation.disambiguateName("npmAggregated")

    val publicPackageJsonConfigurationName
        get() = compilation.disambiguateName("publicPackageJsonConfiguration")

    override fun getAttributes(): AttributeContainer {
        return compilation.attributes
    }

    @Suppress("DEPRECATION")
    @Deprecated("Accessing task instance directly is deprecated", replaceWith = ReplaceWith("compileTaskProvider"))
    override val compileKotlinTask: Kotlin2JsCompile
        get() = compilation.compileKotlinTask as Kotlin2JsCompile

    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    @Deprecated("Replaced with compileTaskProvider", replaceWith = ReplaceWith("compileTaskProvider"))
    override val compileKotlinTaskProvider: TaskProvider<out Kotlin2JsCompile>
        get() = compilation.compileKotlinTaskProvider as TaskProvider<out Kotlin2JsCompile>

    @Suppress("UNCHECKED_CAST")
    override val compileTaskProvider: TaskProvider<Kotlin2JsCompile>
        get() = compilation.compileTaskProvider as TaskProvider<Kotlin2JsCompile>

    internal val packageJsonHandlers = mutableListOf<Action<PackageJson>>()

    fun packageJson(handler: Action<PackageJson>) {
        packageJsonHandlers.add(handler)
    }

    fun packageJson(handler: Closure<*>) {
        packageJson {
            project.configure(this, handler)
        }
    }
}
