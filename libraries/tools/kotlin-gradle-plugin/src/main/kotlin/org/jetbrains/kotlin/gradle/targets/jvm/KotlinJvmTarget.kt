/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.jvm

import org.gradle.api.InvalidUserCodeException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.tasks.Jar
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinOnlyTarget
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.addExtendsFromRelation
import org.jetbrains.kotlin.utils.addToStdlib.cast
import java.util.concurrent.Callable
import javax.inject.Inject
import kotlin.reflect.full.functions

open class KotlinJvmTarget @Inject constructor(
    project: Project
) : KotlinOnlyTarget<KotlinJvmCompilation>(project, KotlinPlatformType.jvm),
    KotlinTargetWithTests<JvmClasspathTestRunSource, KotlinJvmTestRun> {

    override lateinit var testRuns: NamedDomainObjectContainer<KotlinJvmTestRun>

    var withJavaEnabled = false
        private set

    @Suppress("unused") // user DSL
    fun withJava() {
        if (withJavaEnabled)
            return

        project.multiplatformExtension.targets.find { it is KotlinJvmTarget && it.withJavaEnabled }
            ?.let { existingJavaTarget ->
                throw InvalidUserCodeException(
                    "Only one of the JVM targets can be configured to work with Java. The target '${existingJavaTarget.name}' is " +
                            "already set up to work with Java; cannot setup another target '$targetName'"
                )
            }

        withJavaEnabled = true

        project.plugins.apply(JavaPlugin::class.java)
        val javaPluginConvention = project.convention.getPlugin(JavaPluginConvention::class.java)
        AbstractKotlinPlugin.setUpJavaSourceSets(this, duplicateJavaSourceSetsAsKotlinSourceSets = false)

        // Below, some effort is made to ensure that a user or 3rd-party plugin that inspects or interacts
        // with the entities created by the Java plugin, not knowing of the existence of the Kotlin plugin,
        // sees 'the right picture' of the inputs and outputs, for example:
        // * the relevant dependencies for Java and Kotlin are in sync,
        // * the Java outputs contain the outputs produced by Kotlin as well

        javaPluginConvention.sourceSets.all { javaSourceSet ->
            val compilation = compilations.getByName(javaSourceSet.name)
            val compileJavaTask = project.tasks.withType<AbstractCompile>().named(javaSourceSet.compileJavaTaskName)

            setupJavaSourceSetSourcesAndResources(javaSourceSet, compilation)

            val javaClasses = project.files(compileJavaTask.map { it.destinationDir })

            compilation.output.classesDirs.from(javaClasses)

            (javaSourceSet.output.classesDirs as? ConfigurableFileCollection)?.from(
                compilation.output.classesDirs.minus(javaClasses)
            )

            javaSourceSet.output.setResourcesDir(Callable { compilation.output.resourcesDirProvider })

            setupDependenciesCrossInclusionForJava(compilation, javaSourceSet)
        }

        // Eliminate the Java output configurations from dependency resolution to avoid ambiguity between them and
        // the equivalent configurations created for the target:
        listOf(JavaPlugin.API_ELEMENTS_CONFIGURATION_NAME, JavaPlugin.RUNTIME_ELEMENTS_CONFIGURATION_NAME)
            .forEach { outputConfigurationName ->
                project.configurations.findByName(outputConfigurationName)?.isCanBeConsumed = false
            }

        disableJavaPluginTasks(javaPluginConvention)
    }

    private fun setupJavaSourceSetSourcesAndResources(
        javaSourceSet: SourceSet,
        compilation: KotlinJvmCompilation
    ) {
        javaSourceSet.java.setSrcDirs(listOf("src/${compilation.defaultSourceSet.name}/java"))
        compilation.defaultSourceSet.kotlin.srcDirs(javaSourceSet.java.sourceDirectories)

        // To avoid confusion in the sources layout, remove the default Java source directories
        // (like src/main/java, src/test/java) and instead add sibling directories to those where the Kotlin
        // sources are placed (i.e. src/jvmMain/java, src/jvmTest/java):
        javaSourceSet.resources.setSrcDirs(compilation.defaultSourceSet.resources.sourceDirectories)
        compilation.defaultSourceSet.resources.srcDirs(javaSourceSet.resources.sourceDirectories)
        project.tasks.named(
            compilation.processResourcesTaskName,
            ProcessResources::class.java
        ).configure {
            // Now 'compilation' has additional resources dir from java compilation which points to the initial
            // resources location. Because of this, ProcessResources task will copy same files twice,
            // so we are excluding duplicates.
            it.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }

        // Resources processing is done with the Kotlin resource processing task:
        project.tasks.named(javaSourceSet.processResourcesTaskName).configure {
            it.dependsOn(project.tasks.named(compilation.processResourcesTaskName))
            it.enabled = false
        }
    }

    private fun disableJavaPluginTasks(javaPluginConvention: JavaPluginConvention) {
        // A 'normal' build should not do redundant job like running the tests twice or building two JARs,
        // so disable some tasks and just make them depend on the others:
        val targetJar = project.tasks.withType(Jar::class.java).named(artifactsTaskName)

        project.tasks.withType(Jar::class.java).named(javaPluginConvention.sourceSets.getByName("main").jarTaskName) { javaJar ->
            (javaJar.source as? ConfigurableFileCollection)?.setFrom(targetJar.map { it.source })
            javaJar.archiveFileName.set(targetJar.flatMap { it.archiveFileName })
            javaJar.dependsOn(targetJar)
            javaJar.enabled = false
        }

        project.tasks.withType(Test::class.java).named(JavaPlugin.TEST_TASK_NAME) { javaTestTask ->
            javaTestTask.dependsOn(project.tasks.named(testTaskName))
            javaTestTask.enabled = false
        }
    }

    private fun setupDependenciesCrossInclusionForJava(
        compilation: KotlinJvmCompilation,
        javaSourceSet: SourceSet
    ) {
        // Make sure Kotlin compilation dependencies appear in the Java source set classpaths:

        listOfNotNull(
            compilation.apiConfigurationName,
            compilation.implementationConfigurationName,
            compilation.compileOnlyConfigurationName,
            compilation.deprecatedCompileConfigurationName.takeIf { project.configurations.findByName(it) != null }
        ).forEach { configurationName ->
            project.addExtendsFromRelation(javaSourceSet.compileClasspathConfigurationName, configurationName)
        }

        listOfNotNull(
            compilation.apiConfigurationName,
            compilation.implementationConfigurationName,
            compilation.runtimeOnlyConfigurationName,
            compilation.deprecatedRuntimeConfigurationName.takeIf { project.configurations.findByName(it) != null }
        ).forEach { configurationName ->
            project.addExtendsFromRelation(javaSourceSet.runtimeClasspathConfigurationName, configurationName)
        }

        // Add the Java source set dependencies to the Kotlin compilation compile & runtime configurations:

        val compileConfigurationName = if (areRuntimeOrCompileConfigurationsAvailable()) {
            javaSourceSet::class
                .functions
                .find { it.name == "getCompileConfigurationName" }
                ?.call(javaSourceSet)
                ?.cast<String>()
                ?.takeIf { project.configurations.findByName(it) != null }
        } else null

        listOfNotNull(
            compileConfigurationName,
            javaSourceSet.compileOnlyConfigurationName,
            javaSourceSet.apiConfigurationName.takeIf { project.configurations.findByName(it) != null },
            javaSourceSet.implementationConfigurationName
        ).forEach { configurationName ->
            project.addExtendsFromRelation(compilation.compileDependencyConfigurationName, configurationName)
        }

        val runtimeConfigurationName = if (areRuntimeOrCompileConfigurationsAvailable()) {
            javaSourceSet::class
                .functions
                .find { it.name == "getRuntimeConfigurationName" }
                ?.call(javaSourceSet)
                ?.cast<String>()
                ?.takeIf { project.configurations.findByName(it) != null }
        } else null

        listOfNotNull(
            runtimeConfigurationName,
            javaSourceSet.runtimeOnlyConfigurationName,
            javaSourceSet.apiConfigurationName.takeIf { project.configurations.findByName(it) != null },
            javaSourceSet.implementationConfigurationName
        ).forEach { configurationName ->
            project.addExtendsFromRelation(compilation.runtimeDependencyConfigurationName, configurationName)
        }
    }

    /**
     * Check if "compile" and "runtime" configurations are still available in current Gradle version.
     */
    private fun areRuntimeOrCompileConfigurationsAvailable(): Boolean =
        GradleVersion.version(project.gradle.gradleVersion) <= GradleVersion.version("6.8.3")
}

