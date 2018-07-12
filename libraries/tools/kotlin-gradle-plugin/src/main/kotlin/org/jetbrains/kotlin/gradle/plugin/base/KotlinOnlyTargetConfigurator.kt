/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.base

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.artifacts.ArtifactAttributes
import org.gradle.api.internal.artifacts.publish.ArchivePublishArtifact
import org.gradle.api.internal.plugins.DefaultArtifactPublicationSet
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.bundling.Jar
import org.gradle.internal.cleanup.BuildOutputCleanupRegistry
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinOnlyTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.disambiguateName
import org.jetbrains.kotlin.gradle.plugin.mpp.fullName
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.utils.isGradleVersionAtLeast
import java.io.File
import java.util.concurrent.Callable

open class KotlinOnlyTargetConfigurator(
    private val buildOutputCleanupRegistry: BuildOutputCleanupRegistry
) {
    fun <KotlinCompilationType: KotlinCompilation> configureTarget(
        project: Project,
        target: KotlinOnlyTarget<KotlinCompilationType>
    ) {
        configureCompilationDefaults(project, target)
        configureCompilations(project, target)
        defineConfigurationsForTarget(project, target)
        configureArchivesAndComponent(project, target)
        configureBuild(project, target)

        setCompatibilityOfAbstractCompileTasks(project)
    }

    private fun <KotlinCompilationType: KotlinCompilation> configureCompilations(
        project: Project,
        platformTarget: KotlinOnlyTarget<KotlinCompilationType>
    ) {
        val main = platformTarget.compilations.create(KotlinCompilation.MAIN_COMPILATION_NAME)

        platformTarget.compilations.create(KotlinCompilation.TEST_COMPILATION_NAME).apply {
            compileDependencyFiles = project.files(main.output, project.configurations.maybeCreate(compileDependencyConfigurationName))

            if (this is KotlinCompilationToRunnableFiles) {
                runtimeDependencyFiles = project.files(output, main.output, project.configurations.maybeCreate(runtimeDependencyConfigurationName))
            }
        }

        platformTarget.compilations.all {
            buildOutputCleanupRegistry.registerOutputs(it.output)
        }
    }

    private fun <KotlinCompilationType: KotlinCompilation> configureCompilationDefaults(project: Project, target: KotlinOnlyTarget<KotlinCompilationType>) {
        target.compilations.all { compilation ->
            defineConfigurationsForCompilation(compilation, target, project.configurations)

            project.kotlinExtension.sourceSets.maybeCreate(compilation.fullName).also { sourceSet ->
                compilation.source(sourceSet) // also adds dependencies, requires the configurations for target and source set to exist at this point
            }

            if (compilation is KotlinCompilationWithResources) {
                configureResourceProcessing(project, compilation, project.files())
            }

            createLifecycleTask(compilation, project)
        }
    }

    private fun configureArchivesAndComponent(project: Project, target: KotlinOnlyTarget<*>) {
        val mainCompilation = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)

        val jar = project.tasks.create(target.artifactsTaskName, Jar::class.java)
        jar.description = "Assembles a jar archive containing the main classes."
        jar.group = BasePlugin.BUILD_GROUP
        jar.from(mainCompilation.output)

        val jarArtifact = ArchivePublishArtifact(jar)
        val apiElementsConfiguration = project.configurations.getByName(target.apiElementsConfigurationName)

        target.disambiguationClassifier?.let { jar.classifier = it }

        project.extensions.getByType(DefaultArtifactPublicationSet::class.java).addCandidate(jarArtifact)

        addJar(apiElementsConfiguration, jarArtifact)

        if (mainCompilation is KotlinCompilationToRunnableFiles) {
            val runtimeConfiguration = project.configurations.getByName(mainCompilation.deprecatedRuntimeConfigurationName)
            val runtimeElementsConfiguration = project.configurations.getByName(target.runtimeElementsConfigurationName)
            addJar(runtimeConfiguration, jarArtifact)
            addJar(runtimeElementsConfiguration, jarArtifact) // TODO Check Gradle's special split into variants for classes & resources
        }

        // note: there's no variant configuration for now

        // FIXME ensure this dependency through configurations instead:
        project.tasks.getByName("assemble").dependsOn(jar)
    }

    private fun addJar(configuration: Configuration, jarArtifact: ArchivePublishArtifact) {
        val publications = configuration.outgoing

        // Configure an implicit variant
        publications.artifacts.add(jarArtifact)
        publications.attributes.attribute(ArtifactAttributes.ARTIFACT_FORMAT, ArtifactTypeDefinition.JAR_TYPE)
    }

    private fun configureResourceProcessing(
        project: Project,
        compilation: KotlinCompilationWithResources,
        resourceSet: FileCollection
    ) {
        compilation.output.setResourcesDir(Callable {
            val classesDirName = "resources/" + compilation.compilationName
            File(project.buildDir, classesDirName)
        })

        val resourcesTask = project.tasks.maybeCreate(compilation.processResourcesTaskName, ProcessResources::class.java)
        resourcesTask.description = "Processes $resourceSet."
        DslObject(resourcesTask).conventionMapping.map("destinationDir") { compilation.output.resourcesDir }
        resourcesTask.from(resourceSet)
    }

    private fun createLifecycleTask(compilation: KotlinCompilation, project: Project) {
        (compilation.output.classesDirs as ConfigurableFileCollection).from(project.files().builtBy(compilation.compileAllTaskName))

        project.tasks.create(compilation.compileAllTaskName).apply {
            group = LifecycleBasePlugin.BUILD_GROUP
            description = "Assembles " + compilation.output + "."
            dependsOn(
                compilation.output.dirs,
                compilation.compileKotlinTaskName
            )
            if (compilation is KotlinCompilationWithResources) {
                dependsOn(compilation.processResourcesTaskName)
            }
        }
    }

    private val KotlinCompilation.deprecatedCompileConfigurationName: String
        get() = disambiguateName("compile")

    private val KotlinCompilationToRunnableFiles.deprecatedRuntimeConfigurationName: String
        get() = disambiguateName("runtime")

    internal fun defineConfigurationsForCompilation(
        compilation: KotlinCompilation,
        target: KotlinTarget,
        configurations: ConfigurationContainer
    ) {
        val compileConfiguration = configurations.maybeCreate(compilation.deprecatedCompileConfigurationName).apply {
            usesPlatformOf(target)
            isVisible = false
            description = "Dependencies for $compilation (deprecated, use '${compilation.implementationConfigurationName} ' instead)."
        }

        val apiConfiguration = configurations.maybeCreate(compilation.apiConfigurationName).apply {
            extendsFrom(compileConfiguration)
            usesPlatformOf(target)
            isVisible = false
            isCanBeConsumed = false
            isCanBeResolved = false
            description = "API dependencies for $compilation."
        }

        val implementationConfiguration = configurations.maybeCreate(compilation.implementationConfigurationName).apply {
            extendsFrom(compileConfiguration, apiConfiguration)
            usesPlatformOf(target)
            isVisible = false
            isCanBeConsumed = false
            isCanBeResolved = false
            description = "Implementation only dependencies for $compilation."
        }

        val compileOnlyConfiguration = configurations.maybeCreate(compilation.compileOnlyConfigurationName).apply {
            usesPlatformOf(target)
            isVisible = false
            description = "Compile only dependencies for $compilation."
        }

        val compileClasspathConfiguration = configurations.maybeCreate(compilation.compileDependencyConfigurationName).apply {
            extendsFrom(compileOnlyConfiguration, implementationConfiguration)
            usesPlatformOf(target)
            isVisible = false
            isCanBeConsumed = false
            attributes.attribute(USAGE_ATTRIBUTE, compilation.target.project.usageByName(Usage.JAVA_API))
            description = "Compile classpath for $compilation."
        }

        compilation.compileDependencyFiles = compileClasspathConfiguration

        if (compilation is KotlinCompilationToRunnableFiles) {
            val runtimeConfiguration = configurations.maybeCreate(compilation.deprecatedRuntimeConfigurationName).apply {
                extendsFrom(compileConfiguration)
                usesPlatformOf(target)
                isVisible = false
                description =
                        "Runtime dependencies for $compilation (deprecated, use '${compilation.runtimeOnlyConfigurationName} ' instead)."
            }

            val runtimeOnlyConfiguration = configurations.maybeCreate(compilation.runtimeOnlyConfigurationName).apply {
                usesPlatformOf(target)
                isVisible = false
                isCanBeConsumed = false
                isCanBeResolved = false
                description = "Runtime only dependencies for $compilation."
            }

            val runtimeClasspathConfiguration = configurations.maybeCreate(compilation.runtimeDependencyConfigurationName).apply {
                extendsFrom(runtimeOnlyConfiguration, runtimeConfiguration, implementationConfiguration)
                usesPlatformOf(target)
                isVisible = false
                isCanBeConsumed = false
                isCanBeResolved = true
                attributes.attribute(USAGE_ATTRIBUTE, compilation.target.project.usageByName(Usage.JAVA_RUNTIME))
                description = "Runtime classpath of $compilation."
            }

            compilation.runtimeDependencyFiles = compilation.output.plus(runtimeClasspathConfiguration)
        }
    }

    private fun defineConfigurationsForTarget(project: Project, target: KotlinOnlyTarget<*>) {
        val configurations = project.configurations

        val defaultConfiguration = configurations.maybeCreate(target.defaultConfigurationName)
        val mainCompilation = target.compilations.maybeCreate(KotlinCompilation.MAIN_COMPILATION_NAME)
        val testCompilation = target.compilations.maybeCreate(KotlinCompilation.TEST_COMPILATION_NAME)

        val compileConfiguration = configurations.maybeCreate(mainCompilation.deprecatedCompileConfigurationName)
        val implementationConfiguration = configurations.maybeCreate(mainCompilation.implementationConfigurationName)

        val runtimeOnlyConfiguration = configurations.maybeCreate(mainCompilation.runtimeOnlyConfigurationName)
        val compileTestsConfiguration = configurations.maybeCreate(testCompilation.deprecatedCompileConfigurationName)
        val testImplementationConfiguration = configurations.maybeCreate(testCompilation.implementationConfigurationName)
        val testRuntimeOnlyConfiguration = configurations.maybeCreate(testCompilation.runtimeOnlyConfigurationName)

        compileTestsConfiguration.extendsFrom(compileConfiguration).usesPlatformOf(target)
        testImplementationConfiguration.extendsFrom(implementationConfiguration).usesPlatformOf(target)
        testRuntimeOnlyConfiguration.extendsFrom(runtimeOnlyConfiguration).usesPlatformOf(target)

        configurations.maybeCreate(target.apiElementsConfigurationName).apply {
            description = "API elements for main."
            isVisible = false
            isCanBeResolved = false
            isCanBeConsumed = true
            attributes.attribute<Usage>(USAGE_ATTRIBUTE, project.usageByName(Usage.JAVA_API))
            extendsFrom(configurations.maybeCreate(mainCompilation.apiConfigurationName))
            if (mainCompilation is KotlinCompilationToRunnableFiles) {
                val runtimeConfiguration = configurations.maybeCreate(mainCompilation.deprecatedRuntimeConfigurationName)
                extendsFrom(runtimeConfiguration)
            }
            usesPlatformOf(target)
        }

        val runtimeElementsConfiguration = configurations.maybeCreate(target.runtimeElementsConfigurationName).apply {
            description = "Elements of runtime for main."
            isVisible = false
            isCanBeConsumed = true
            isCanBeResolved = false
            attributes.attribute<Usage>(USAGE_ATTRIBUTE, project.usageByName(Usage.JAVA_RUNTIME_JARS))
            if (mainCompilation is KotlinCompilationToRunnableFiles) {
                val runtimeConfiguration = configurations.maybeCreate(mainCompilation.deprecatedRuntimeConfigurationName)
                extendsFrom(implementationConfiguration, runtimeOnlyConfiguration, runtimeConfiguration)
            }
            usesPlatformOf(target)
        }

        if (mainCompilation is KotlinCompilationToRunnableFiles && testCompilation is KotlinCompilationToRunnableFiles) {
            val runtimeConfiguration = configurations.maybeCreate(mainCompilation.deprecatedRuntimeConfigurationName)
            val testRuntimeConfiguration = configurations.maybeCreate(testCompilation.deprecatedRuntimeConfigurationName)
            testRuntimeConfiguration.extendsFrom(runtimeConfiguration).usesPlatformOf(target)
        }

        defaultConfiguration.extendsFrom(runtimeElementsConfiguration).usesPlatformOf(target)
    }


    private fun configureBuild(project: Project, target: KotlinOnlyTarget<*>) {
        val testCompilation = target.compilations.getByName(KotlinCompilation.TEST_COMPILATION_NAME)
        project.tasks.maybeCreate(buildNeededTaskName, DefaultTask::class.java).apply {
            description = "Assembles and tests this project and all projects it depends on."
            group = "build"
            dependsOn("build")
            if (testCompilation is KotlinCompilationToRunnableFiles) {
                addDependsOnTaskInOtherProjects(this@apply, true, name, testCompilation.deprecatedRuntimeConfigurationName)
            }
        }

        project.tasks.maybeCreate(buildDependentTaskName, DefaultTask::class.java).apply {
            setDescription("Assembles and tests this project and all projects that depend on it.")
            setGroup("build")
            dependsOn("build")
            doFirst {
                if (!project.gradle.includedBuilds.isEmpty()) {
                    project.logger.warn("[composite-build] Warning: `" + path + "` task does not build included builds.")
                }
            }
            if (testCompilation is KotlinCompilationToRunnableFiles) {
                addDependsOnTaskInOtherProjects(this@apply, false, name, testCompilation.deprecatedRuntimeConfigurationName)
            }
        }
    }

    private fun addDependsOnTaskInOtherProjects(
        task: Task, useDependedOn: Boolean, otherProjectTaskName: String,
        configurationName: String
    ) {
        val project = task.project
        val configuration = project.configurations.getByName(configurationName)
        task.dependsOn(configuration.getTaskDependencyFromProjectDependency(useDependedOn, otherProjectTaskName))
    }

    private fun setCompatibilityOfAbstractCompileTasks(project: Project) = with (project) {
        tasks.withType(AbstractKotlinCompile::class.java).all {
            // Workaround: these are input properties and should not hold null values:
            it.targetCompatibility = ""
            it.sourceCompatibility = ""
        }
    }


    internal companion object {
        const val buildNeededTaskName = "buildAllNeeded"
        const val buildDependentTaskName = "buildAllDependents"
    }
}

internal fun Project.usageByName(usageName: String): Usage =
    if (isGradleVersionAtLeast(4, 0)) {
        // `project.objects` is an API introduced in Gradle 4.0
        project.objects.named(Usage::class.java, usageName)
    } else {
        val usagesClass = Class.forName("org.gradle.api.internal.attributes")
        val usagesMethod = usagesClass.getMethod("usage", String::class.java)
        usagesMethod(null, usageName) as Usage
    }

fun Configuration.usesPlatformOf(target: KotlinTarget): Configuration {
    attributes.attribute(KotlinPlatformType.attribute, target.platformType)
    return this
}