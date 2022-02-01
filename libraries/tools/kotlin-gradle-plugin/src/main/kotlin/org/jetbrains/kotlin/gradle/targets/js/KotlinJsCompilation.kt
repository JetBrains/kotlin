/*
* Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
* Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
*/

@file:Suppress("PackageDirectoryMismatch")

// Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import groovy.lang.Closure
import org.gradle.api.tasks.TaskProvider
import org.gradle.util.ConfigureUtil
import org.gradle.util.WrapUtil
import org.jetbrains.kotlin.gradle.dsl.KotlinJsOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationWithResources
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsSubTargetContainerDsl
import org.jetbrains.kotlin.gradle.targets.js.dukat.ExternalsOutputFormat
import org.jetbrains.kotlin.gradle.targets.js.ir.JsBinary
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsBinaryContainer
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJson
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

open class KotlinJsCompilation internal constructor(
    compilationDetails: JsCompilationDetails
) : AbstractKotlinCompilationToRunnableFiles<KotlinJsOptions>(compilationDetails),
    KotlinCompilationWithResources<KotlinJsOptions> {

    final override val target: KotlinTarget get() = super.target

    constructor(target: KotlinTarget, name: String) : this(JsCompilationDetails(target, name))

    private val kotlinProperties = PropertiesProvider(target.project)

    internal open val externalsOutputFormat: ExternalsOutputFormat
        get() = kotlinProperties.externalsOutputFormat ?: defaultExternalsOutputFormat

    internal open val defaultExternalsOutputFormat: ExternalsOutputFormat = ExternalsOutputFormat.SOURCE

    internal val binaries: KotlinJsBinaryContainer =
        target.project.objects.newInstance(
            KotlinJsBinaryContainer::class.java,
            target,
            WrapUtil.toDomainObjectSet(JsBinary::class.java)
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

    override val compileKotlinTask: Kotlin2JsCompile
        get() = super.compileKotlinTask as Kotlin2JsCompile

    @Suppress("UNCHECKED_CAST")
    override val compileKotlinTaskProvider: TaskProvider<out Kotlin2JsCompile>
        get() = super.compileKotlinTaskProvider as TaskProvider<out Kotlin2JsCompile>

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