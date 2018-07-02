/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.base

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.artifacts.ArtifactAttributes
import org.gradle.api.internal.artifacts.publish.ArchivePublishArtifact
import org.gradle.api.internal.plugins.DefaultArtifactPublicationSet
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.ReportingBasePlugin
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.internal.cleanup.BuildOutputCleanupRegistry
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinOnlyTarget
import org.jetbrains.kotlin.gradle.plugin.source.KotlinSourceSet
import java.io.File
import javax.inject.Inject

open class KotlinOnlyTargetConfigurator(
    private val buildOutputCleanupRegistry: BuildOutputCleanupRegistry,
    private val objectFactory: ObjectFactory
) {
    fun <KotlinCompilationType: KotlinCompilation> configureTarget(
        project: Project,
        kotlinPlatformExtension: KotlinOnlyTarget<KotlinCompilationType>
    ) {
        configureCompilationDefaults(project, kotlinPlatformExtension)
        configureSourceSets(project, kotlinPlatformExtension)
        defineConfigurationsForTarget(project, kotlinPlatformExtension)
        configureArchivesAndComponent(project, kotlinPlatformExtension)
        configureBuild(project, kotlinPlatformExtension)

        // TODO: find a more reasonable workaround?
        setCompatibilityOfAbstractCompileTasks(project)
    }

    private fun <KotlinCompilationType: KotlinCompilation> configureSourceSets(
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
            val outputConventionMapping = DslObject(compilation.output).conventionMapping

            val configurations = project.configurations

            if (compilation is KotlinCompilationWithResources) {
                configureResourceProcessing(project, compilation, compilation.resources, project)
            }

            defineConfigurationsForCompilation(compilation, target, configurations)

            createLifecycleTask(compilation, project)
        }
    }

    private fun configureArchivesAndComponent(project: Project, platformExtension: KotlinOnlyTarget<*>) {
        val jar = project.tasks.create(platformExtension.jarTaskName, Jar::class.java)
        jar.description = "Assembles a jar archive containing the main classes."
        jar.group = BasePlugin.BUILD_GROUP
        jar.from(platformExtension.sourceSets.getByName(mainSourceSetName).output)

        val jarArtifact = ArchivePublishArtifact(jar)
        val apiElementsConfiguration = project.configurations.getByName(platformExtension.apiElementsConfigurationName)
        val runtimeConfiguration = project.configurations.getByName(platformExtension.runtimeConfigurationName)

        platformExtension.platformDisambiguationClassifier?.let { jar.classifier = it }

        project.extensions.getByType(DefaultArtifactPublicationSet::class.java).addCandidate(jarArtifact)

        addJar(apiElementsConfiguration, jarArtifact)
        addJar(runtimeConfiguration, jarArtifact)
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
        resourceSet: SourceDirectorySet,
        target: Project
    ) {
        DslObject(compilation.output).conventionMapping.map("resourcesDir") {
            val classesDirName = "resources/" + compilation.name
            File(project.buildDir, classesDirName)
        }

        compilation.resources.srcDir("src/" + compilation.name + "/resources")

        //TODO replace maybeCreate with create, as there won't be Java plugin
        val resourcesTask = target.tasks.maybeCreate(compilation.processResourcesTaskName, ProcessResources::class.java)
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

    private fun defineConfigurationsForCompilation(
        compilation: KotlinCompilation,
        platformExtension: KotlinOnlyTarget<*>,
        configurations: ConfigurationContainer
    ) {
        val compileConfiguration = configurations.maybeCreate(compilation.compileConfigurationName)
        compileConfiguration.isVisible = false
        compileConfiguration.description = "Dependencies for $sourceSet (deprecated, use '${sourceSet.implementationConfigurationName} ' instead)."

        val implementationConfiguration = configurations.maybeCreate(sourceSet.implementationConfigurationName).apply {
            extendsFrom(compileConfiguration)
            usesPlatformOf(platformExtension)
            isVisible = false
            isCanBeConsumed = false
            isCanBeResolved = false
            description = "Implementation only dependencies for $sourceSet."
        }

        val runtimeConfiguration = configurations.maybeCreate(sourceSet.runtimeConfigurationName).apply {
            extendsFrom(compileConfiguration)
            usesPlatformOf(platformExtension)
            isVisible = false
            description = "Runtime dependencies for $sourceSet (deprecated, use '${sourceSet.runtimeOnlyConfigurationName} ' instead)."
        }

        val compileOnlyConfiguration = configurations.maybeCreate(sourceSet.compileConfigurationName).apply {
            usesPlatformOf(platformExtension)
            isVisible = false
            description = "Compile only dependencies for $sourceSet."
        }

        val compileClasspathConfiguration = configurations.maybeCreate(sourceSet.compileClasspathConfigurationName).apply {
            extendsFrom(compileOnlyConfiguration, implementationConfiguration)
            usesPlatformOf(platformExtension)
            isVisible = false
            isCanBeConsumed = false
            attributes.attribute(USAGE_ATTRIBUTE, objectFactory.named(Usage::class.java, Usage.JAVA_API))
            description = "Compile classpath for $sourceSet."
        }

        val runtimeOnlyConfiguration = configurations.maybeCreate(sourceSet.runtimeOnlyConfigurationName).apply {
            usesPlatformOf(platformExtension)
            isVisible = false
            isCanBeConsumed = false
            isCanBeResolved = false
            description = "Runtime only dependencies for $sourceSet."
        }

        val runtimeClasspathConfiguration = configurations.maybeCreate(sourceSet.runtimeClasspathConfigurationName).apply {
            extendsFrom(runtimeOnlyConfiguration, runtimeConfiguration, implementationConfiguration)
            usesPlatformOf(platformExtension)
            isVisible = false
            isCanBeConsumed = false
            isCanBeResolved = true
            attributes.attribute(USAGE_ATTRIBUTE, objectFactory.named(Usage::class.java, Usage.JAVA_RUNTIME))
            description = "Runtime classpath of $sourceSet."
        }

        sourceSet.compileClasspath = compileClasspathConfiguration
        sourceSet.runtimeClasspath = sourceSet.output.plus(runtimeClasspathConfiguration)
    }

    private fun defineConfigurationsForTarget(project: Project, target: KotlinOnlyTarget<*>) {
        val configurations = project.configurations

        val defaultConfiguration = configurations.maybeCreate(target.defaultConfigurationName)

        val implementationConfiguration = configurations.maybeCreate(target.implementationConfigurationName)
        val runtimeConfiguration = configurations.maybeCreate(platformExtension.runtimeConfigurationName)
        val runtimeOnlyConfiguration = configurations.maybeCreate(platformExtension.runtimeOnlyConfigurationName)
        val compileTestsConfiguration = configurations.maybeCreate(platformExtension.testCompileConfigurationName)
        val testImplementationConfiguration = configurations.maybeCreate(platformExtension.testImplementationConfigurationName)
        val testRuntimeConfiguration = configurations.maybeCreate(platformExtension.testRuntimeConfigurationName)
        val testRuntimeOnlyConfiguration = configurations.maybeCreate(platformExtension.testRuntimeOnlyConfigurationName)

        compileTestsConfiguration.extendsFrom(compileConfiguration).usesPlatformOf(platformExtension)
        testImplementationConfiguration.extendsFrom(implementationConfiguration).usesPlatformOf(platformExtension)
        testRuntimeConfiguration.extendsFrom(runtimeConfiguration).usesPlatformOf(platformExtension)
        testRuntimeOnlyConfiguration.extendsFrom(runtimeOnlyConfiguration).usesPlatformOf(platformExtension)

        val usageAttribute = PlatformConfigurationUsage.attributeForModule(project)

        configurations.maybeCreate(platformExtension.apiElementsConfigurationName).apply {
            description = "API elements for main."
            isVisible = false
            isCanBeResolved = false
            isCanBeConsumed = true
            attributes.attribute<Usage>(
                USAGE_ATTRIBUTE,
                objectFactory.named(Usage::class.java, Usage.JAVA_API))
            extendsFrom(runtimeConfiguration)
            usesPlatformOf(platformExtension)
            attributes.attribute(usageAttribute, PlatformConfigurationUsage.PLATFORM_IMPLEMENTATION)
        }

        val runtimeElementsConfiguration = configurations.maybeCreate(platformExtension.runtimeElementsConfigurationName).apply {
            description = "Elements of runtime for main."
            isVisible = false
            isCanBeConsumed = true
            isCanBeResolved = false
            attributes.attribute<Usage>(
                USAGE_ATTRIBUTE,
                objectFactory.named(Usage::class.java, Usage.JAVA_RUNTIME_JARS)
            )
            extendsFrom(implementationConfiguration, runtimeOnlyConfiguration, runtimeConfiguration)
            usesPlatformOf(platformExtension)
            attributes.attribute(usageAttribute, PlatformConfigurationUsage.PLATFORM_IMPLEMENTATION)
        }

        defaultConfiguration.extendsFrom(runtimeElementsConfiguration).usesPlatformOf(platformExtension)
    }


    private fun configureBuild(project: Project, platformExtension: KotlinOnlyTarget) {
        project.tasks.maybeCreate(buildNeededTaskName, DefaultTask::class.java).apply {
            description = "Assembles and tests this project and all projects it depends on."
            group = "build"
            dependsOn("build")
            addDependsOnTaskInOtherProjects(this@apply, true, name, platformExtension.testRuntimeConfigurationName)
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
            addDependsOnTaskInOtherProjects(this@apply, false, name, platformExtension.testRuntimeConfigurationName)
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
        tasks.withType(AbstractCompile::class.java).all {
            it.targetCompatibility = ""
            it.sourceCompatibility = ""
        }
    }


    internal companion object {
        const val buildNeededTaskName = "buildAllNeeded"
        const val buildDependentTaskName = "buildAllDependents"

        // We need both to be able to seamlessly use a single implementation for a certain platform as a dependency in a module which
        // has more than one implmentation for the same platform
        val kotlinPlatformTypeAttribute = Attribute.of("org.jetbrains.kotlin.platform.type", KotlinPlatformType::class.java)
        val kotlinPlatformIdentifierAttribute = Attribute.of("org.jetbrains.kotlin.platform.identifier", String::class.java)
    }
}

internal fun Configuration.usesPlatformOf(extension: KotlinTarget): Configuration {
    if (extension is KotlinOnlyTarget<*>) {
        extension.userDefinedPlatformId?.let {
            attributes.attribute(KotlinOnlyTargetConfigurator.kotlinPlatformIdentifierAttribute, it)
        }
    }
    attributes.attribute(KotlinOnlyTargetConfigurator.kotlinPlatformTypeAttribute, extension.platformType)
    return this
}

class KotlinOnlyPlugin @Inject constructor(
    private val buildOutputCleanupRegistry: BuildOutputCleanupRegistry,
    private val objectFactory: ObjectFactory
) : Plugin<Project> {

    override fun apply(project: Project) {
        with(project.pluginManager) {
            apply(BasePlugin::class.java)
            apply(ReportingBasePlugin::class.java)
            // TODO check that the functionality of JavaBasePlugin is correctly copied
        }

        val kotlinTarget = KotlinOnlyTarget `
        KotlinOnlyTargetConfigurator(buildOutputCleanupRegistry, objectFactory).configureTarget(project, kotlinPlatformExtension)
    }
}