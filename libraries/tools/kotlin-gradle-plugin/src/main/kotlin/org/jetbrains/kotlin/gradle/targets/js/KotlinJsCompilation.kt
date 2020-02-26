/*
* Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
* Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
*/

@file:Suppress("PackageDirectoryMismatch")

// Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.util.WrapUtil
import org.jetbrains.kotlin.gradle.dsl.KotlinJsOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationWithResources
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.JsIrBinary
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsBinaryContainer
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJson
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.utils.*

open class KotlinJsCompilation(
    target: KotlinTarget,
    name: String
) : AbstractKotlinCompilationToRunnableFiles<KotlinJsOptions>(target, name), KotlinCompilationWithResources<KotlinJsOptions> {
    internal val binaries: KotlinJsBinaryContainer =
        target.project.objects.newInstance(
            KotlinJsBinaryContainer::class.java,
            target,
            WrapUtil.toDomainObjectSet(JsIrBinary::class.java)
        )

    override val processResourcesTaskName: String
        get() = disambiguateName("processResources")

    override val compileKotlinTask: Kotlin2JsCompile
        get() = super.compileKotlinTask as Kotlin2JsCompile

    internal val packageJsonHandlers = mutableListOf<PackageJson.() -> Unit>()

    @Suppress("unused")
    fun packageJson(handler: PackageJson.() -> Unit) {
        packageJsonHandlers.add(handler)
    }

    override val apiConfigurationName: String
        get() = disambiguateNameInPlatform(API)

    override val implementationConfigurationName: String
        get() = disambiguateNameInPlatform(IMPLEMENTATION)

    override val compileOnlyConfigurationName: String
        get() = disambiguateNameInPlatform(COMPILE_ONLY)

    override val runtimeOnlyConfigurationName: String
        get() = disambiguateNameInPlatform(RUNTIME_ONLY)

    protected open val disambiguationClassifierInPlatform: String?
        get() = (target as KotlinJsTarget).disambiguationClassifierInPlatform

    private fun disambiguateNameInPlatform(simpleName: String): String {
        return lowerCamelCaseName(
            disambiguationClassifierInPlatform,
            compilationName.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME },
            simpleName
        )
    }

    override val defaultSourceSetName: String
        get() {
            val target = target as KotlinJsTarget
            return lowerCamelCaseName(
                target.irTarget?.let {
                    target.disambiguationClassifierInPlatform
                } ?: target.disambiguationClassifier,
                compilationName
            )
        }
}