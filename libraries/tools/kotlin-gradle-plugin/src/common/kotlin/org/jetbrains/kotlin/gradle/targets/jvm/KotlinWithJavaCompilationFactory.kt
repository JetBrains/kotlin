/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch", "TYPEALIAS_EXPANSION_DEPRECATION") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.DeprecatedHasCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationLanguageSettingsConfigurator
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinJvmCompilationAssociator
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory.JvmWithJavaCompilationDependencyConfigurationsFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory.JvmWithJavaCompilationTaskNamesContainerFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory.KotlinCompilationImplFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory.plus
import org.jetbrains.kotlin.gradle.utils.filesProvider
import org.jetbrains.kotlin.gradle.utils.javaSourceSets

@Suppress("DEPRECATION")
class KotlinWithJavaCompilationFactory<KotlinOptionsType : KotlinCommonOptions, CO : KotlinCommonCompilerOptions> internal constructor(
    override val target: KotlinWithJavaTarget<KotlinOptionsType, CO>,
    val compilerOptionsFactory: () -> DeprecatedHasCompilerOptions<CO>,
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
            preConfigureAction = KotlinCompilationLanguageSettingsConfigurator + { compilation ->
                compilation.compileDependencyFiles = project.filesProvider { javaSourceSet.compileClasspath }
                compilation.runtimeDependencyFiles = project.filesProvider { javaSourceSet.runtimeClasspath }
            },
        )

        return project.objects.newInstance(
            KotlinWithJavaCompilation::class.java, compilationImplFactory.create(target, name), javaSourceSet
        ) as KotlinWithJavaCompilation<KotlinOptionsType, CO>
    }
}
