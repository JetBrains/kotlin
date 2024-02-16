/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.AfterFinaliseDsl
import org.jetbrains.kotlin.gradle.plugin.internal.JavaSourceSetsAccessor
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationImpl
import org.jetbrains.kotlin.gradle.plugin.variantImplementationFactory
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.utils.CompletableFuture
import org.jetbrains.kotlin.gradle.utils.Future
import org.jetbrains.kotlin.gradle.utils.lenient
import javax.inject.Inject

@Suppress("TYPEALIAS_EXPANSION_DEPRECATION")
open class KotlinJvmCompilation @Inject internal constructor(
    compilation: KotlinCompilationImpl,
) : DeprecatedAbstractKotlinCompilationToRunnableFiles<KotlinJvmOptions>(compilation),
    DeprecatedKotlinCompilationWithResources<KotlinJvmOptions> {

    final override val target: KotlinJvmTarget = compilation.target as KotlinJvmTarget

    @Suppress("DEPRECATION")
    @Deprecated(
        "To configure compilation compiler options use 'compileTaskProvider':\ncompilation.compileTaskProvider.configure{\n" +
                "    compilerOptions {}\n}"
    )
    override val compilerOptions: DeprecatedHasCompilerOptions<KotlinJvmCompilerOptions> =
        compilation.compilerOptions.castCompilerOptionsType()

    @Deprecated("Replaced with compileTaskProvider", replaceWith = ReplaceWith("compileTaskProvider"))
    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    override val compileKotlinTaskProvider: TaskProvider<out org.jetbrains.kotlin.gradle.tasks.KotlinCompile>
        get() = compilation.compileKotlinTaskProvider as TaskProvider<out org.jetbrains.kotlin.gradle.tasks.KotlinCompile>

    @Suppress("DEPRECATION")
    @Deprecated("Accessing task instance directly is deprecated", replaceWith = ReplaceWith("compileTaskProvider"))
    override val compileKotlinTask: org.jetbrains.kotlin.gradle.tasks.KotlinCompile
        get() = compilation.compileKotlinTask as org.jetbrains.kotlin.gradle.tasks.KotlinCompile

    @Suppress("UNCHECKED_CAST")
    override val compileTaskProvider: TaskProvider<out KotlinCompilationTask<KotlinJvmCompilerOptions>>
        get() = compilation.compileTaskProvider as TaskProvider<KotlinCompilationTask<KotlinJvmCompilerOptions>>

    /**
     * **Note**: requesting this too early (right after target creation and before any target configuration) may falsely return `null`
     * value, but later target will be configured to run with Java enabled. If possible, please use [compileJavaTaskProviderSafe].
     */
    val compileJavaTaskProvider: TaskProvider<out JavaCompile>?
        get() = if (target.withJavaEnabled) {
            val project = target.project
            val javaSourceSets = project.variantImplementationFactory<JavaSourceSetsAccessor.JavaSourceSetsAccessorVariantFactory>()
                .getInstance(project)
                .sourceSets
            val javaSourceSet = javaSourceSets.getByName(compilationName)
            project.tasks.withType(JavaCompile::class.java).named(javaSourceSet.compileJavaTaskName)
        } else null

    /**
     * Alternative to [compileJavaTaskProvider] to safely receive [JavaCompile] task provider  when [KotlinJvmTarget.withJavaEnabled]
     * will be enabled after call to this method.
     */
    internal val compileJavaTaskProviderSafe: Provider<JavaCompile> = target.project.providers
        .provider { javaSourceSet.lenient.getOrNull() }
        .flatMap { javaSourceSet ->
            checkNotNull(javaSourceSet)
            project.tasks.named(javaSourceSet.compileJavaTaskName, JavaCompile::class.java)
        }


    internal val javaSourceSet: Future<SourceSet?> get() = javaSourceSetImpl
    private val javaSourceSetImpl: CompletableFuture<SourceSet?> = CompletableFuture<SourceSet?>().also { future ->
        /**
         * If no SourceSet was set until 'AfterFinaliseDsl', then user really did never call into 'withJava', hence
         * we can complete the Future with 'null' notifying everybody, that there won't be any java source set associated with
         * this compilation
         */
        target.project.launchInStage(AfterFinaliseDsl) {
            if (!future.isCompleted) {
                future.complete(null)
            }
        }
    }

    internal fun maybeCreateJavaSourceSet(): SourceSet {
        check(target.withJavaEnabled)
        val sourceSet = target.project.javaSourceSets.maybeCreate(compilationName)
        javaSourceSetImpl.complete(sourceSet)
        return sourceSet
    }

    override val processResourcesTaskName: String
        get() = compilation.processResourcesTaskName ?: error("Missing 'processResourcesTaskName'")
}
