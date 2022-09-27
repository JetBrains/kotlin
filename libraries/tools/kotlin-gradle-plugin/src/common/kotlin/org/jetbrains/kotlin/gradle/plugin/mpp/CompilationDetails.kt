/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Action
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.*
import org.jetbrains.kotlin.gradle.plugin.sources.*
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.tooling.core.closure
import java.util.*
import javax.inject.Inject

interface CompilationDetails<T : KotlinCommonOptions> {
    val target: KotlinTarget

    val compileDependencyFilesHolder: GradleKpmDependencyFilesHolder

    val kotlinDependenciesHolder: HasKotlinDependencies

    val compilationData: KotlinCompilationData<T>

    fun associateWith(other: CompilationDetails<*>)
    val associateCompilations: Set<CompilationDetails<*>>

    fun source(sourceSet: KotlinSourceSet)

    val directlyIncludedKotlinSourceSets: ObservableSet<KotlinSourceSet>

    val allKotlinSourceSets: ObservableSet<KotlinSourceSet>

    val defaultSourceSet: KotlinSourceSet

    @Deprecated("Use defaultSourceSet.name instead", ReplaceWith("defaultSourceSet.name"), level = DeprecationLevel.WARNING)
    val defaultSourceSetName: String get() = defaultSourceSet.name

    @Suppress("UNCHECKED_CAST")
    val compilation: KotlinCompilation<T>
        get() = target.compilations.getByName(compilationData.compilationPurpose) as KotlinCompilation<T>
}

interface CompilationDetailsWithRuntime<T : KotlinCommonOptions> : CompilationDetails<T> {
    val runtimeDependencyFilesHolder: GradleKpmDependencyFilesHolder
}

internal val CompilationDetails<*>.associateCompilationsClosure: Iterable<CompilationDetails<*>>
    get() = closure { it.associateCompilations }


internal open class JsCompilationDetails(
    target: KotlinTarget,
    compilationPurpose: String,
    defaultSourceSet: KotlinSourceSet,
) : DefaultCompilationDetailsWithRuntime<KotlinJsOptions, KotlinJsCompilerOptions>(
    target,
    compilationPurpose,
    defaultSourceSet,
    {
        object : HasCompilerOptions<KotlinJsCompilerOptions> {
            override val options: KotlinJsCompilerOptions =
                target.project.objects.newInstance(KotlinJsCompilerOptionsDefault::class.java)
        }
    },
    {
        object : KotlinJsOptions {
            override val options: KotlinJsCompilerOptions
                get() = compilerOptions.options
        }
    }
) {

    internal abstract class JsCompilationDependenciesHolder @Inject constructor(
        val target: KotlinTarget,
        val compilationPurpose: String
    ) : HasKotlinDependencies {
        override val apiConfigurationName: String
            get() = disambiguateNameInPlatform(API)

        override val implementationConfigurationName: String
            get() = disambiguateNameInPlatform(IMPLEMENTATION)

        override val compileOnlyConfigurationName: String
            get() = disambiguateNameInPlatform(COMPILE_ONLY)

        override val runtimeOnlyConfigurationName: String
            get() = disambiguateNameInPlatform(RUNTIME_ONLY)

        protected open val disambiguationClassifierInPlatform: String?
            get() = when (target) {
                is KotlinJsTarget -> target.disambiguationClassifierInPlatform
                is KotlinJsIrTarget -> target.disambiguationClassifierInPlatform
                else -> error("Unexpected target type of $target")
            }

        private fun disambiguateNameInPlatform(simpleName: String): String {
            return lowerCamelCaseName(
                disambiguationClassifierInPlatform,
                compilationPurpose.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME },
                "compilation",
                simpleName
            )
        }

        override fun dependencies(configure: KotlinDependencyHandler.() -> Unit): Unit =
            DefaultKotlinDependencyHandler(this, target.project).run(configure)

        override fun dependencies(configure: Action<KotlinDependencyHandler>) =
            dependencies { configure.execute(this) }
    }

    override val kotlinDependenciesHolder: HasKotlinDependencies
        get() = target.project.objects.newInstance(JsCompilationDependenciesHolder::class.java, target, compilationPurpose)

}

internal class JsIrCompilationDetails(
    target: KotlinTarget, compilationPurpose: String, defaultSourceSet: KotlinSourceSet
) : JsCompilationDetails(target, compilationPurpose, defaultSourceSet) {

    internal abstract class JsIrCompilationDependencyHolder @Inject constructor(target: KotlinTarget, compilationPurpose: String) :
        JsCompilationDependenciesHolder(target, compilationPurpose) {
        override val disambiguationClassifierInPlatform: String?
            get() = (target as KotlinJsIrTarget).disambiguationClassifierInPlatform
    }

    override val kotlinDependenciesHolder: HasKotlinDependencies
        get() = target.project.objects.newInstance(JsIrCompilationDependencyHolder::class.java, target, compilationPurpose)
}

internal abstract class KotlinDependencyConfigurationsHolder @Inject constructor(
    val project: Project,
    private val configurationNamesPrefix: String?,
) : HasKotlinDependencies {

    override val apiConfigurationName: String
        get() = lowerCamelCaseName(configurationNamesPrefix, API)

    override val implementationConfigurationName: String
        get() = lowerCamelCaseName(configurationNamesPrefix, IMPLEMENTATION)

    override val compileOnlyConfigurationName: String
        get() = lowerCamelCaseName(configurationNamesPrefix, COMPILE_ONLY)

    override val runtimeOnlyConfigurationName: String
        get() = lowerCamelCaseName(configurationNamesPrefix, RUNTIME_ONLY)

    override fun dependencies(configure: KotlinDependencyHandler.() -> Unit): Unit =
        DefaultKotlinDependencyHandler(this, project).run(configure)

    override fun dependencies(configure: Action<KotlinDependencyHandler>) =
        dependencies { configure.execute(this) }
}
