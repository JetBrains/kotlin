/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.tasks.SourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.InternalKotlinCompilation
import org.jetbrains.kotlin.gradle.utils.filesProvider

internal fun interface KotlinCompilationAssociator {
    fun associate(target: KotlinTarget, first: InternalKotlinCompilation<*>, second: InternalKotlinCompilation<*>)
}

internal object DefaultKotlinCompilationAssociator : KotlinCompilationAssociator {
    override fun associate(target: KotlinTarget, first: InternalKotlinCompilation<*>, second: InternalKotlinCompilation<*>) {
        target.project.kotlinCompilationModuleManager.unionModules(first.compilationModule, second.compilationModule)
        val project = target.project

        /*
          we add dependencies to compileDependencyConfiguration ('compileClasspath' usually) and runtimeDependency
          ('runtimeClasspath') instead of modifying respective api/implementation/compileOnly/runtimeOnly configs

          This is needed because api/implementation/compileOnly/runtimeOnly are used in IDE Import and will leak
          to dependencies of IDE modules. But they are not needed here, because IDE resolution works inherently
          transitively and symbols from associated compilation will be resolved from source sets of associated
          compilation itself (moreover, direct dependencies are not equivalent to transitive ones because of
          resolution order - e.g. in case of FQNs clash, so it's even harmful)
        */
        project.dependencies.add(first.compileOnlyConfigurationName, project.files({ second.output.classesDirs }))
        project.dependencies.add(first.runtimeOnlyConfigurationName, project.files({ second.output.allOutputs }))

        first.compileDependencyConfigurationName.addAllDependenciesFromOtherConfigurations(
            project,
            second.apiConfigurationName,
            second.implementationConfigurationName,
            second.compileOnlyConfigurationName
        )

        first.runtimeDependencyConfigurationName?.addAllDependenciesFromOtherConfigurations(
            project,
            second.apiConfigurationName,
            second.implementationConfigurationName,
            second.runtimeOnlyConfigurationName
        )
    }

    /**
     * Adds `allDependencies` of configurations mentioned in `configurationNames` to configuration named [this] in
     * a lazy manner
     */
    private fun String.addAllDependenciesFromOtherConfigurations(project: Project, vararg configurationNames: String) {
        project.configurations.named(this).configure { receiverConfiguration ->
            receiverConfiguration.dependencies.addAllLater(
                project.objects.listProperty(Dependency::class.java).apply {
                    set(
                        project.provider {
                            configurationNames
                                .map { project.configurations.getByName(it) }
                                .flatMap { it.allDependencies }
                        }
                    )
                }
            )
        }
    }
}

internal object KotlinNativeCompilationAssociator : KotlinCompilationAssociator {
    override fun associate(target: KotlinTarget, first: InternalKotlinCompilation<*>, second: InternalKotlinCompilation<*>) {
        target.project.kotlinCompilationModuleManager.unionModules(first.compilationModule, second.compilationModule)

        first.compileDependencyFiles +=
            second.output.classesDirs + target.project.filesProvider { second.compileDependencyFiles }

        target.project.configurations.named(first.implementationConfigurationName).configure { configuration ->
            configuration.extendsFrom(target.project.configurations.findByName(second.implementationConfigurationName))
        }
    }
}

internal object KotlinJvmWithJavaCompilationAssociator : KotlinCompilationAssociator {
    override fun associate(target: KotlinTarget, first: InternalKotlinCompilation<*>, second: InternalKotlinCompilation<*>) {
        target.project.kotlinCompilationModuleManager.unionModules(first.compilationModule, second.compilationModule)
        if (first.compilationName != SourceSet.TEST_SOURCE_SET_NAME || second.compilationName != SourceSet.MAIN_SOURCE_SET_NAME) {
            DefaultKotlinCompilationAssociator.associate(target, first, second)
        } // otherwise, do nothing: the Java Gradle plugin adds these dependencies for us, we don't need to add them to the classpath
    }
}


