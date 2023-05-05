/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.jvm
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.InternalKotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinWithJavaTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.plugin.mpp.isTest
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.utils.filesProvider

internal fun interface KotlinCompilationAssociator {
    fun associate(target: KotlinTarget, auxiliary: InternalKotlinCompilation<*>, main: InternalKotlinCompilation<*>)
}

internal object DefaultKotlinCompilationAssociator : KotlinCompilationAssociator {
    override fun associate(target: KotlinTarget, auxiliary: InternalKotlinCompilation<*>, main: InternalKotlinCompilation<*>) {
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
        project.dependencies.add(auxiliary.compileOnlyConfigurationName, project.files({ main.output.classesDirs }))
        project.dependencies.add(auxiliary.runtimeOnlyConfigurationName, project.files({ main.output.allOutputs }))
        // Adding classes that could be produced into non-default destination for JVM target
        // Check KotlinSourceSetProcessor for details
        project.dependencies.add(
            auxiliary.implementationConfigurationName,
            project.objects.fileCollection().from(
                {
                    main.defaultSourceSet.kotlin.classesDirectory.orNull?.asFile
                }
            )
        )

        auxiliary.compileDependencyConfigurationName.addAllDependenciesFromOtherConfigurations(
            project,
            main.apiConfigurationName,
            main.implementationConfigurationName,
            main.compileOnlyConfigurationName
        )

        auxiliary.runtimeDependencyConfigurationName?.addAllDependenciesFromOtherConfigurations(
            project,
            main.apiConfigurationName,
            main.implementationConfigurationName,
            main.runtimeOnlyConfigurationName
        )
    }
}

internal object KotlinNativeCompilationAssociator : KotlinCompilationAssociator {
    override fun associate(target: KotlinTarget, auxiliary: InternalKotlinCompilation<*>, main: InternalKotlinCompilation<*>) {

        auxiliary.compileDependencyFiles +=
            main.output.classesDirs + target.project.filesProvider { main.compileDependencyFiles }

        target.project.configurations.named(auxiliary.implementationConfigurationName).configure { configuration ->
            configuration.extendsFrom(target.project.configurations.findByName(main.implementationConfigurationName))
        }
    }
}

internal object KotlinJvmCompilationAssociator : KotlinCompilationAssociator {
    override fun associate(target: KotlinTarget, auxiliary: InternalKotlinCompilation<*>, main: InternalKotlinCompilation<*>) {
        /* Main to Test association handled already by java plugin */
        if (
            ((target is KotlinWithJavaTarget<*, *> && target.platformType == jvm) ||
                    (target is KotlinJvmTarget && target.withJavaEnabled)) &&
            auxiliary.isTest() && main.isMain()
        ) {
            return
        } else DefaultKotlinCompilationAssociator.associate(target, auxiliary, main)
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
    override fun associate(target: KotlinTarget, auxiliary: InternalKotlinCompilation<*>, main: InternalKotlinCompilation<*>) {
        auxiliary.compileDependencyConfigurationName.addAllDependenciesFromOtherConfigurations(
            target.project,
            main.apiConfigurationName,
            main.implementationConfigurationName,
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