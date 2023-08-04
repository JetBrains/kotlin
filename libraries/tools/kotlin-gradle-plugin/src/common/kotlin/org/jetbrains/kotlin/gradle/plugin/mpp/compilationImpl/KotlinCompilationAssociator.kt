/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.SourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.jvm
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.utils.Future
import org.jetbrains.kotlin.gradle.utils.filesProvider
import org.jetbrains.kotlin.gradle.utils.future
import org.jetbrains.kotlin.gradle.utils.listProperty


internal fun interface KotlinCompilationAssociator {
    fun associate(target: KotlinTarget, auxiliary: InternalKotlinCompilation<*>, main: InternalKotlinCompilation<*>)
}

internal object DefaultKotlinCompilationAssociator : KotlinCompilationAssociator {
    override fun associate(target: KotlinTarget, auxiliary: InternalKotlinCompilation<*>, main: InternalKotlinCompilation<*>) {
        val project = target.project

        /*
        This associator has two jobs
        1) It will add the output of the 'main' compilation as compile & runtime dependency to the 'auxiliary' compilation
        2) It will add all 'declared dependencies' present on 'main' to 'auxiliary'

        For 1)
        This is necessary so that all symbols that are declared/produced in 'main' are available in 'auxiliary'.
        We use the 'compileOnlyConfiguration' and 'runtimeOnlyConfigurationName' to add the respective classes.
            Note (a): This 'associate' function will be called for 'all' associated compilations (full transitive closure)
            Note (b): It is important that the compiled output of 'main' is prioritised in the compile path order:
                      We therefore ensure that the files are added to the front of the compile path.

                      This is necessary as other binaries might leak into the compile path which contain the same symbols but
                      are not marked as 'friend'. We ensure that associate dependencies are resolved first

        For 2)
        This is an agreed upon convention: 'test' is able to see all dependencies declared for 'main'
        As described in 1b: It needs to be taken care of, that the dependencies are ordered after the output of 'main'
        */
        project.dependencies.add(auxiliary.compileOnlyConfigurationName, main.output.classesDirs)
        project.dependencies.add(auxiliary.runtimeOnlyConfigurationName, main.output.allOutputs)

        // Adding classes that could be produced into non-default destination for JVM target
        // Check KotlinSourceSetProcessor for details
        project.dependencies.add(
            auxiliary.implementationConfigurationName,
            project.filesProvider { main.defaultSourceSet.kotlin.classesDirectory.orNull?.asFile }
        )

        // Adding declared dependencies
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
            project.listProperty<Dependency> {
                configurationNames
                    .map { project.configurations.getByName(it) }
                    .flatMap { it.allDependencies }
            }
        )
    }
}
