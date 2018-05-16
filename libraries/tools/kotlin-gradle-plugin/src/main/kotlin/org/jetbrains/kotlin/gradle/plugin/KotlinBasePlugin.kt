/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.ConventionMapping
import org.gradle.api.internal.artifacts.ArtifactAttributes
import org.gradle.api.internal.artifacts.publish.ArchivePublishArtifact
import org.gradle.api.internal.plugins.DefaultArtifactPublicationSet
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPlugin.*
import org.gradle.api.plugins.ReportingBasePlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.bundling.Jar
import org.gradle.internal.cleanup.BuildOutputCleanupRegistry
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinExtendedSourceSet
import java.io.File
import javax.inject.Inject

class KotlinBasePlugin @Inject constructor(
    private val buildOutputCleanupRegistry: BuildOutputCleanupRegistry,
    private val objectFactory: ObjectFactory
) : Plugin<Project> {

    override fun apply(project: Project) {
        with(project.pluginManager) {
            apply(BasePlugin::class.java)
            apply(ReportingBasePlugin::class.java)
            apply(JavaBasePlugin::class.java)
        }

        val kotlinProjectExtension = project.extensions.getByType(KotlinProjectExtension::class.java)

        configureSourceSetDefaults(project, kotlinProjectExtension)
        configureSourceSets(project, kotlinProjectExtension)

        configureConfigurations(project)

        configureArchivesAndComponent(project)

        configureBuild(project)
    }

    private fun configureSourceSets(project: Project, kotlinProjectExtension: KotlinProjectExtension) {
        val main = kotlinProjectExtension.sourceSets.create(SourceSet.MAIN_SOURCE_SET_NAME)

        kotlinProjectExtension.sourceSets.create(SourceSet.TEST_SOURCE_SET_NAME).apply {
            compileClasspath = project.files(main.output, project.configurations.getByName(TEST_COMPILE_CLASSPATH_CONFIGURATION_NAME))
            runtimeClasspath = project.files(output, main.output, project.configurations.getByName(TEST_RUNTIME_CLASSPATH_CONFIGURATION_NAME))
        }

        kotlinProjectExtension.sourceSets.all {
            buildOutputCleanupRegistry.registerOutputs(it.output)
        }
    }

    private fun configureSourceSetDefaults(project: Project, kotlinProjectExtension: KotlinProjectExtension) {
        kotlinProjectExtension.sourceSets.all { sourceSet ->
            val outputConventionMapping = DslObject(sourceSet.output).conventionMapping

            val configurations = project.configurations

            definePathsForSourceSet(sourceSet, outputConventionMapping, project)
            defineConfigurationsForSourceSet(sourceSet, configurations)

            createProcessResourcesTask(sourceSet, sourceSet.resources, project)
            createLifecycleTask(sourceSet, project)
        }
    }

    private fun configureArchivesAndComponent(project: Project) {
        val jar = project.tasks.create(JAR_TASK_NAME, Jar::class.java)
        jar.description = "Assembles a jar archive containing the main classes."
        jar.group = BasePlugin.BUILD_GROUP
        jar.from(project.kotlinExtension.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).output)

        val jarArtifact = ArchivePublishArtifact(jar)
        val apiElementConfiguration = project.configurations.getByName(API_ELEMENTS_CONFIGURATION_NAME)
        val runtimeConfiguration = project.configurations.getByName(RUNTIME_CONFIGURATION_NAME)

        project.extensions.getByType(DefaultArtifactPublicationSet::class.java).addCandidate(jarArtifact)

        addJar(apiElementConfiguration, jarArtifact)
        addJar(runtimeConfiguration, jarArtifact)
        // note: there's no variant configuration for now
    }

    private fun addJar(configuration: Configuration, jarArtifact: ArchivePublishArtifact) {
        val publications = configuration.outgoing

        // Configure an implicit variant
        publications.artifacts.add(jarArtifact)
        publications.attributes.attribute(ArtifactAttributes.ARTIFACT_FORMAT, ArtifactTypeDefinition.JAR_TYPE)
    }

    private fun definePathsForSourceSet(sourceSet: KotlinExtendedSourceSet, outputConventionMapping: ConventionMapping, project: Project) {
        outputConventionMapping.map("resourcesDir") {
            val classesDirName = "resources/" + sourceSet.name
            File(project.buildDir, classesDirName)
        }

        sourceSet.resources.srcDir("src/" + sourceSet.name + "/resources")
    }

    private fun createProcessResourcesTask(sourceSet: KotlinExtendedSourceSet, resourceSet: SourceDirectorySet, target: Project) {
        //TODO replace maybeCreate with create, as there won't be Java plugin
        val resourcesTask = target.tasks.maybeCreate(sourceSet.processResourcesTaskName, ProcessResources::class.java)
        resourcesTask.description = "Processes $resourceSet."
        DslObject(resourcesTask).conventionMapping.map("destinationDir") { sourceSet.output.resourcesDir }
        resourcesTask.from(resourceSet)
    }

    private fun createLifecycleTask(sourceSet: KotlinExtendedSourceSet, target: Project) {
        sourceSet.compiledBy(sourceSet.classesTaskName)

        //TODO replace maybeCreate with create
        val classesTask = target.tasks.maybeCreate(sourceSet.classesTaskName)
        classesTask.group = LifecycleBasePlugin.BUILD_GROUP
        classesTask.description = "Assembles " + sourceSet.output + "."
        classesTask.dependsOn(sourceSet.output.dirs)
        classesTask.dependsOn(sourceSet.compileKotlinTaskName)
        classesTask.dependsOn(sourceSet.processResourcesTaskName)
    }

    private fun defineConfigurationsForSourceSet(sourceSet: KotlinExtendedSourceSet, configurations: ConfigurationContainer) {
        val compileConfigurationName = sourceSet.compileConfigurationName
        val implementationConfigurationName = sourceSet.implementationConfigurationName
        val runtimeConfigurationName = sourceSet.runtimeConfigurationName
        val runtimeOnlyConfigurationName = sourceSet.runtimeOnlyConfigurationName
        val compileOnlyConfigurationName = sourceSet.compileConfigurationName
        val compileClasspathConfigurationName = sourceSet.compileClasspathConfigurationName
        val runtimeClasspathConfigurationName = sourceSet.runtimeClasspathConfigurationName
        val sourceSetName = sourceSet.toString()

        val compileConfiguration = configurations.maybeCreate(compileConfigurationName)
        compileConfiguration.setVisible(false)
        compileConfiguration.setDescription("Dependencies for $sourceSetName (deprecated, use '$implementationConfigurationName ' instead).")

        val implementationConfiguration = configurations.maybeCreate(implementationConfigurationName)
        implementationConfiguration.setVisible(false)
        implementationConfiguration.setDescription("Implementation only dependencies for $sourceSetName.")
        implementationConfiguration.setCanBeConsumed(false)
        implementationConfiguration.setCanBeResolved(false)
        implementationConfiguration.extendsFrom(compileConfiguration)

        val runtimeConfiguration = configurations.maybeCreate(runtimeConfigurationName)
        runtimeConfiguration.setVisible(false)
        runtimeConfiguration.extendsFrom(compileConfiguration)
        runtimeConfiguration.setDescription("Runtime dependencies for $sourceSetName (deprecated, use '$runtimeOnlyConfigurationName ' instead).")

        val compileOnlyConfiguration = configurations.maybeCreate(compileOnlyConfigurationName)
        compileOnlyConfiguration.setVisible(false)
        compileOnlyConfiguration.setDescription("Compile only dependencies for $sourceSetName.")

        val compileClasspathConfiguration = configurations.maybeCreate(compileClasspathConfigurationName)
        compileClasspathConfiguration.setVisible(false)
        compileClasspathConfiguration.extendsFrom(compileOnlyConfiguration, implementationConfiguration)
        compileClasspathConfiguration.setDescription("Compile classpath for $sourceSetName.")
        compileClasspathConfiguration.setCanBeConsumed(false)
        compileClasspathConfiguration.getAttributes().attribute(USAGE_ATTRIBUTE, objectFactory.named(Usage::class.java, Usage.JAVA_API))

        val runtimeOnlyConfiguration = configurations.maybeCreate(runtimeOnlyConfigurationName)
        runtimeOnlyConfiguration.setVisible(false)
        runtimeOnlyConfiguration.setCanBeConsumed(false)
        runtimeOnlyConfiguration.setCanBeResolved(false)
        runtimeOnlyConfiguration.setDescription("Runtime only dependencies for $sourceSetName.")

        val runtimeClasspathConfiguration = configurations.maybeCreate(runtimeClasspathConfigurationName)
        runtimeClasspathConfiguration.setVisible(false)
        runtimeClasspathConfiguration.setCanBeConsumed(false)
        runtimeClasspathConfiguration.setCanBeResolved(true)
        runtimeClasspathConfiguration.setDescription("Runtime classpath of $sourceSetName.")
        runtimeClasspathConfiguration.extendsFrom(runtimeOnlyConfiguration, runtimeConfiguration, implementationConfiguration)
        runtimeClasspathConfiguration.getAttributes().attribute(USAGE_ATTRIBUTE, objectFactory.named(Usage::class.java, Usage.JAVA_RUNTIME))

        sourceSet.compileClasspath = compileClasspathConfiguration
        sourceSet.runtimeClasspath = sourceSet.output.plus(runtimeClasspathConfiguration)
    }

    private fun configureConfigurations(project: Project) {
        val configurations = project.configurations

        val defaultConfiguration = configurations.getByName(Dependency.DEFAULT_CONFIGURATION)
        val compileConfiguration = configurations.getByName(COMPILE_CONFIGURATION_NAME)
        val implementationConfiguration = configurations.getByName(IMPLEMENTATION_CONFIGURATION_NAME)
        val runtimeConfiguration = configurations.getByName(RUNTIME_CONFIGURATION_NAME)
        val runtimeOnlyConfiguration = configurations.getByName(RUNTIME_ONLY_CONFIGURATION_NAME)
        val compileTestsConfiguration = configurations.getByName(TEST_COMPILE_CONFIGURATION_NAME)
        val testImplementationConfiguration = configurations.getByName(TEST_IMPLEMENTATION_CONFIGURATION_NAME)
        val testRuntimeConfiguration = configurations.getByName(TEST_RUNTIME_CONFIGURATION_NAME)
        val testRuntimeOnlyConfiguration = configurations.getByName(TEST_RUNTIME_ONLY_CONFIGURATION_NAME)

        compileTestsConfiguration.extendsFrom(compileConfiguration)
        testImplementationConfiguration.extendsFrom(implementationConfiguration)
        testRuntimeConfiguration.extendsFrom(runtimeConfiguration)
        testRuntimeOnlyConfiguration.extendsFrom(runtimeOnlyConfiguration)

        val apiElementsConfiguration = configurations.maybeCreate(API_ELEMENTS_CONFIGURATION_NAME)
        apiElementsConfiguration.isVisible = false
        apiElementsConfiguration.description = "API elements for main."
        apiElementsConfiguration.isCanBeResolved = false
        apiElementsConfiguration.isCanBeConsumed = true
        apiElementsConfiguration.attributes.attribute<Usage>(USAGE_ATTRIBUTE, objectFactory.named(Usage::class.java, Usage.JAVA_API))
        apiElementsConfiguration.extendsFrom(runtimeConfiguration)

        val runtimeElementsConfiguration = configurations.maybeCreate(RUNTIME_ELEMENTS_CONFIGURATION_NAME)
        runtimeElementsConfiguration.isVisible = false
        runtimeElementsConfiguration.isCanBeConsumed = true
        runtimeElementsConfiguration.isCanBeResolved = false
        runtimeElementsConfiguration.description = "Elements of runtime for main."
        runtimeElementsConfiguration.attributes.attribute<Usage>(
            USAGE_ATTRIBUTE,
            objectFactory.named(Usage::class.java, Usage.JAVA_RUNTIME_JARS)
        )
        runtimeElementsConfiguration.extendsFrom(implementationConfiguration, runtimeOnlyConfiguration, runtimeConfiguration)

        defaultConfiguration.extendsFrom(runtimeElementsConfiguration)
    }

    private fun addDependsOnTaskInOtherProjects(
        task: Task, useDependedOn: Boolean, otherProjectTaskName: String,
        configurationName: String
    ) {
        val project = task.project
        val configuration = project.configurations.getByName(configurationName)
        task.dependsOn(configuration.getTaskDependencyFromProjectDependency(useDependedOn, otherProjectTaskName))
    }

    private fun configureBuild(project: Project) {
        addDependsOnTaskInOtherProjects(
            project.tasks.getByName(JavaBasePlugin.BUILD_NEEDED_TASK_NAME), true,
            JavaBasePlugin.BUILD_NEEDED_TASK_NAME, TEST_RUNTIME_CONFIGURATION_NAME
        )
        addDependsOnTaskInOtherProjects(
            project.tasks.getByName(JavaBasePlugin.BUILD_DEPENDENTS_TASK_NAME), false,
            JavaBasePlugin.BUILD_DEPENDENTS_TASK_NAME, TEST_RUNTIME_CONFIGURATION_NAME
        )
    }


}