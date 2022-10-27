/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.InternalKotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.plugin.mpp.isTest
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.utils.filesProvider

internal fun interface KotlinCompilationAssociator {
    fun associate(target: KotlinTarget, first: InternalKotlinCompilation<*>, second: InternalKotlinCompilation<*>)
}

internal object DefaultKotlinCompilationAssociator : KotlinCompilationAssociator {
    override fun associate(target: KotlinTarget, first: InternalKotlinCompilation<*>, second: InternalKotlinCompilation<*>) {
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
}

internal object KotlinNativeCompilationAssociator : KotlinCompilationAssociator {
    override fun associate(target: KotlinTarget, first: InternalKotlinCompilation<*>, second: InternalKotlinCompilation<*>) {

        first.compileDependencyFiles +=
            second.output.classesDirs + target.project.filesProvider { second.compileDependencyFiles }

        target.project.configurations.named(first.implementationConfigurationName).configure { configuration ->
            configuration.extendsFrom(target.project.configurations.findByName(second.implementationConfigurationName))
        }
    }
}

internal object KotlinJvmCompilationAssociator : KotlinCompilationAssociator {
    override fun associate(target: KotlinTarget, first: InternalKotlinCompilation<*>, second: InternalKotlinCompilation<*>) {
        /* Main to Test association handled already by java plugin */
        if (target is KotlinJvmTarget && target.withJavaEnabled && first.isMain() && second.isTest()) {
            return
        } else DefaultKotlinCompilationAssociator.associate(target, first, second)
    }
}

/*
* Example of how multiplatform dependencies from common would get to Android test classpath:
* commonMainImplementation -> androidDebugImplementation -> debugImplementation -> debugAndroidTestCompileClasspath
* After the fix for KT-35916 MPP compilation configurations receive a 'compilation' postfix for disambiguation.
* androidDebugImplementation remains a source set configuration, but no longer contains compilation dependencies.
* Therefore, it doesn't get dependencies from common source sets.
* We now explicitly add associate compilation dependencies to the Kotlin test compilation configurations (test classpaths).
* This helps, because the Android test classpath configurations extend from the Kotlin test compilations' directly.
*/
internal object KotlinAndroidCompilationAssociator : KotlinCompilationAssociator {
    override fun associate(target: KotlinTarget, first: InternalKotlinCompilation<*>, second: InternalKotlinCompilation<*>) {
        first.compileDependencyConfigurationName.addAllDependenciesFromOtherConfigurations(
            target.project,
            second.apiConfigurationName,
            second.implementationConfigurationName,
            second.compileOnlyConfigurationName
        )
    }
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