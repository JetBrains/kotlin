/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.*
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

open class KotlinOnlyPlatformConfigurator(
    private val buildOutputCleanupRegistry: BuildOutputCleanupRegistry,
    private val objectFactory: ObjectFactory
) {
    fun configureKotlinPlatform(project: Project, kotlinPlatformExtension: KotlinOnlyPlatformExtension) {
        configureSourceSetDefaults(project, kotlinPlatformExtension)
        configureSourceSets(project, kotlinPlatformExtension)
        configureConfigurations(project, kotlinPlatformExtension)
        configureArchivesAndComponent(project, kotlinPlatformExtension)
        configureBuild(project, kotlinPlatformExtension)

        // TODO: find a more reasonable workaround?
        setCompatibilityOfAbstractCompileTasks(project)
    }

    private fun configureSourceSets(project: Project, platformExtension: KotlinOnlyPlatformExtension) {
        val main = platformExtension.sourceSets.create(platformExtension.mainSourceSetName)

        platformExtension.sourceSets.create(platformExtension.testSourceSetName).apply {
            compileClasspath = project.files(main.output, project.configurations.getByName(TEST_COMPILE_CLASSPATH_CONFIGURATION_NAME))
            runtimeClasspath = project.files(output, main.output, project.configurations.getByName(TEST_RUNTIME_CLASSPATH_CONFIGURATION_NAME))
        }

        platformExtension.sourceSets.all {
            buildOutputCleanupRegistry.registerOutputs(it.output)
        }
    }

    private fun configureSourceSetDefaults(project: Project, platformExtension: KotlinOnlyPlatformExtension) {
        platformExtension.sourceSets.all { sourceSet ->
            val outputConventionMapping = DslObject(sourceSet.output).conventionMapping

            val configurations = project.configurations

            definePathsForSourceSet(sourceSet, outputConventionMapping, project)
            defineConfigurationsForSourceSet(sourceSet, configurations)

            createProcessResourcesTask(sourceSet, sourceSet.resources, project)
            createLifecycleTask(sourceSet, project)
        }
    }

    private fun configureArchivesAndComponent(project: Project, platformExtension: KotlinOnlyPlatformExtension) {
        val jar = project.tasks.create(platformExtension.jarTaskName, Jar::class.java)
        jar.description = "Assembles a jar archive containing the main classes."
        jar.group = BasePlugin.BUILD_GROUP
        jar.from(platformExtension.sourceSets.getByName(platformExtension.mainSourceSetName).output)

        val jarArtifact = ArchivePublishArtifact(jar)
        val apiElementsConfiguration = project.configurations.getByName(platformExtension.apiElementsConfigurationName)
        val runtimeConfiguration = project.configurations.getByName(platformExtension.runtimeConfigurationName)

        project.extensions.getByType(DefaultArtifactPublicationSet::class.java).addCandidate(jarArtifact)

        addJar(apiElementsConfiguration, jarArtifact)
        addJar(runtimeConfiguration, jarArtifact)
        // note: there's no variant configuration for now
    }

    private fun addJar(configuration: Configuration, jarArtifact: ArchivePublishArtifact) {
        val publications = configuration.outgoing

        // Configure an implicit variant
        publications.artifacts.add(jarArtifact)
        publications.attributes.attribute(ArtifactAttributes.ARTIFACT_FORMAT, ArtifactTypeDefinition.JAR_TYPE)
    }

    private fun definePathsForSourceSet(sourceSet: KotlinBaseSourceSet, outputConventionMapping: ConventionMapping, project: Project) {
        outputConventionMapping.map("resourcesDir") {
            val classesDirName = "resources/" + sourceSet.name
            File(project.buildDir, classesDirName)
        }

        sourceSet.resources.srcDir("src/" + sourceSet.name + "/resources")
    }

    private fun createProcessResourcesTask(sourceSet: KotlinBaseSourceSet, resourceSet: SourceDirectorySet, target: Project) {
        //TODO replace maybeCreate with create, as there won't be Java plugin
        val resourcesTask = target.tasks.maybeCreate(sourceSet.processResourcesTaskName, ProcessResources::class.java)
        resourcesTask.description = "Processes $resourceSet."
        DslObject(resourcesTask).conventionMapping.map("destinationDir") { sourceSet.output.resourcesDir }
        resourcesTask.from(resourceSet)
    }

    private fun createLifecycleTask(sourceSet: KotlinBaseSourceSet, target: Project) {
        sourceSet.compiledBy(sourceSet.classesTaskName)

        //TODO replace maybeCreate with create
        target.tasks.maybeCreate(sourceSet.classesTaskName).apply {
            group = LifecycleBasePlugin.BUILD_GROUP
            description = "Assembles " + sourceSet.output + "."
            dependsOn(
                sourceSet.output.dirs,
                sourceSet.compileKotlinTaskName,
                sourceSet.processResourcesTaskName
            )
        }
    }

    private fun defineConfigurationsForSourceSet(sourceSet: KotlinBaseSourceSet, configurations: ConfigurationContainer) {
        val compileConfiguration = configurations.maybeCreate(sourceSet.compileConfigurationName)
        compileConfiguration.isVisible = false
        compileConfiguration.description = "Dependencies for $sourceSet (deprecated, use '${sourceSet.implementationConfigurationName} ' instead)."

        val implementationConfiguration = configurations.maybeCreate(sourceSet.implementationConfigurationName).apply {
            extendsFrom(compileConfiguration)
            isVisible = false
            isCanBeConsumed = false
            isCanBeResolved = false
            description = "Implementation only dependencies for $sourceSet."
        }

        val runtimeConfiguration = configurations.maybeCreate(sourceSet.runtimeConfigurationName).apply {
            extendsFrom(compileConfiguration)
            isVisible = false
            description = "Runtime dependencies for $sourceSet (deprecated, use '${sourceSet.runtimeOnlyConfigurationName} ' instead)."
        }

        val compileOnlyConfiguration = configurations.maybeCreate(sourceSet.compileConfigurationName).apply {
            isVisible = false
            description = "Compile only dependencies for $sourceSet."
        }

        val compileClasspathConfiguration = configurations.maybeCreate(sourceSet.compileClasspathConfigurationName).apply {
            extendsFrom(compileOnlyConfiguration, implementationConfiguration)
            isVisible = false
            isCanBeConsumed = false
            attributes.attribute(USAGE_ATTRIBUTE, objectFactory.named(Usage::class.java, Usage.JAVA_API))
            description = "Compile classpath for $sourceSet."
        }

        val runtimeOnlyConfiguration = configurations.maybeCreate(sourceSet.runtimeOnlyConfigurationName).apply {
            isVisible = false
            isCanBeConsumed = false
            isCanBeResolved = false
            description = "Runtime only dependencies for $sourceSet."
        }

        val runtimeClasspathConfiguration = configurations.maybeCreate(sourceSet.runtimeClasspathConfigurationName).apply {
            extendsFrom(runtimeOnlyConfiguration, runtimeConfiguration, implementationConfiguration)
            isVisible = false
            isCanBeConsumed = false
            isCanBeResolved = true
            attributes.attribute(USAGE_ATTRIBUTE, objectFactory.named(Usage::class.java, Usage.JAVA_RUNTIME))
            description = "Runtime classpath of $sourceSet."
        }

        sourceSet.compileClasspath = compileClasspathConfiguration
        sourceSet.runtimeClasspath = sourceSet.output.plus(runtimeClasspathConfiguration)
    }

    private fun configureConfigurations(project: Project, platformExtension: KotlinPlatformExtension) {
        val configurations = project.configurations

        val defaultConfiguration = configurations.getByName(platformExtension.defaultConfigurationName)
        val compileConfiguration = configurations.getByName(platformExtension.compileConfigurationName)
        val implementationConfiguration = configurations.getByName(platformExtension.implementationConfigurationName)
        val runtimeConfiguration = configurations.getByName(platformExtension.runtimeConfigurationName)
        val runtimeOnlyConfiguration = configurations.getByName(platformExtension.runtimeOnlyConfigurationName)
        val compileTestsConfiguration = configurations.getByName(platformExtension.testCompileConfigurationName)
        val testImplementationConfiguration = configurations.getByName(platformExtension.testImplementationConfigurationName)
        val testRuntimeConfiguration = configurations.getByName(platformExtension.testRuntimeConfigurationName)
        val testRuntimeOnlyConfiguration = configurations.getByName(platformExtension.testRuntimeOnlyConfigurationName)

        compileTestsConfiguration.extendsFrom(compileConfiguration)
        testImplementationConfiguration.extendsFrom(implementationConfiguration)
        testRuntimeConfiguration.extendsFrom(runtimeConfiguration)
        testRuntimeOnlyConfiguration.extendsFrom(runtimeOnlyConfiguration)

        configurations.maybeCreate(platformExtension.apiElementsConfigurationName).apply {
            description = "API elements for main."
            isVisible = false
            isCanBeResolved = false
            isCanBeConsumed = true
            attributes.attribute<Usage>(
                USAGE_ATTRIBUTE,
                objectFactory.named(Usage::class.java, Usage.JAVA_API))
            extendsFrom(runtimeConfiguration)
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
        }

        defaultConfiguration.extendsFrom(runtimeElementsConfiguration)
    }


    private fun configureBuild(project: Project, platformExtension: KotlinPlatformExtension) {
        project.tasks.maybeCreate(JavaBasePlugin.BUILD_NEEDED_TASK_NAME, DefaultTask::class.java).apply {
            description = "Assembles and tests this project and all projects it depends on."
            group = "build"
            dependsOn("build")
            addDependsOnTaskInOtherProjects(this@apply, true, name, platformExtension.testRuntimeConfigurationName)
        }

        project.tasks.maybeCreate("buildDependents", DefaultTask::class.java).apply {
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
}

class KotlinOnlyPlugin @Inject constructor(
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
}