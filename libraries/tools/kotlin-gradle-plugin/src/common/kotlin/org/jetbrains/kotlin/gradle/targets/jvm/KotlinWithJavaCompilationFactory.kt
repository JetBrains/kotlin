/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.HasCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.DefaultKotlinCompilationPreConfigure
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinJvmCompilationAssociator
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory.JvmWithJavaCompilationDependencyConfigurationsFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory.JvmWithJavaCompilationTaskNamesContainerFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory.KotlinCompilationImplFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory.plus
import org.jetbrains.kotlin.gradle.utils.filesProvider

class KotlinWithJavaCompilationFactory<KotlinOptionsType : KotlinCommonOptions, CO : KotlinCommonCompilerOptions> internal constructor(
    override val target: KotlinWithJavaTarget<KotlinOptionsType, CO>,
    val compilerOptionsFactory: () -> HasCompilerOptions<CO>,
    val kotlinOptionsFactory: (CO) -> KotlinOptionsType
) : KotlinCompilationFactory<KotlinWithJavaCompilation<KotlinOptionsType, CO>> {

    @Suppress("UNCHECKED_CAST")
    override val itemClass: Class<KotlinWithJavaCompilation<KotlinOptionsType, CO>>
        get() = KotlinWithJavaCompilation::class.java as Class<KotlinWithJavaCompilation<KotlinOptionsType, CO>>

    @Suppress("UNCHECKED_CAST")
    override fun create(name: String): KotlinWithJavaCompilation<KotlinOptionsType, CO> {
        val javaSourceSet = project.javaSourceSets.findByName(name) ?: run {
            /*
            Creating the java SourceSet first here:
            After the javaSourceSet is created, another .all hook will call into this factory creating the KotlinCompilation.
            This call will just return this instance instead eagerly
             */
            project.javaSourceSets.create(name)
            return target.compilations.getByName(name)
        }

        val compilationImplFactory = KotlinCompilationImplFactory(
            processResourcesTaskNameFactory = { _, _ -> javaSourceSet.processResourcesTaskName },
            compilerOptionsFactory = { _, _ ->
                val compilerOptions = compilerOptionsFactory()
                val kotlinOptions = kotlinOptionsFactory(compilerOptions.options)
                KotlinCompilationImplFactory.KotlinCompilerOptionsFactory.Options(compilerOptions, kotlinOptions)
            },
            compilationAssociator = KotlinJvmCompilationAssociator,
            compilationOutputFactory = { _, compilationName ->
                KotlinWithJavaCompilationOutput(project.javaSourceSets.maybeCreate(compilationName))
            },
            compilationDependencyConfigurationsFactory = JvmWithJavaCompilationDependencyConfigurationsFactory(target),
            compilationTaskNamesContainerFactory = JvmWithJavaCompilationTaskNamesContainerFactory(javaSourceSet),

            /* Use compile & runtime classpath from javaSourceSet by default */
            preConfigureAction = DefaultKotlinCompilationPreConfigure + { compilation ->
                compilation.compileDependencyFiles = project.filesProvider { javaSourceSet.compileClasspath }
                compilation.runtimeDependencyFiles = project.filesProvider { javaSourceSet.runtimeClasspath }
            },
        )

        return project.objects.newInstance(
            KotlinWithJavaCompilation::class.java, compilationImplFactory.create(target, name), javaSourceSet
        ) as KotlinWithJavaCompilation<KotlinOptionsType, CO>
    }
}
