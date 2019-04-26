/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.artifacts.ArtifactAttributes
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.Test
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import java.util.concurrent.Callable

abstract class AbstractKotlinTargetConfigurator<KotlinTargetType : KotlinTarget>(
    protected val createDefaultSourceSets: Boolean,
    protected val createTestCompilation: Boolean
) {
    open fun configureTarget(
        target: KotlinTargetType
    ) {
        configureCompilationDefaults(target)
        configureCompilations(target)
        defineConfigurationsForTarget(target)
        configureArchivesAndComponent(target)
        configureTest(target)
        configureBuild(target)
    }


    abstract protected fun configureArchivesAndComponent(target: KotlinTargetType)
    abstract protected fun configureTest(target: KotlinTargetType)

    private fun Project.registerOutputsForStaleOutputCleanup(kotlinCompilation: KotlinCompilation<*>) {
        val cleanTask = tasks.getByName(LifecycleBasePlugin.CLEAN_TASK_NAME) as Delete
        cleanTask.delete(kotlinCompilation.output.allOutputs)
    }

    protected open fun configureCompilations(platformTarget: KotlinTargetType) {
        val project = platformTarget.project
        val main = platformTarget.compilations.create(KotlinCompilation.MAIN_COMPILATION_NAME)

        platformTarget.compilations.all {
            project.registerOutputsForStaleOutputCleanup(it)
            it.compileDependencyFiles = project.configurations.maybeCreate(it.compileDependencyConfigurationName)
            if (it is KotlinCompilationToRunnableFiles) {
                it.runtimeDependencyFiles = project.configurations.maybeCreate(it.runtimeDependencyConfigurationName)
            }
        }

        if (createTestCompilation) {
            platformTarget.compilations.create(KotlinCompilation.TEST_COMPILATION_NAME).apply {
                compileDependencyFiles = project.files(
                    main.output.allOutputs,
                    project.configurations.maybeCreate(compileDependencyConfigurationName)
                )

                if (this is KotlinCompilationToRunnableFiles) {
                    runtimeDependencyFiles = project.files(
                        output.allOutputs,
                        main.output.allOutputs,
                        project.configurations.maybeCreate(runtimeDependencyConfigurationName)
                    )
                }
            }
        }
    }

    protected fun configureCompilationDefaults(target: KotlinTargetType) {
        val project = target.project

        target.compilations.all { compilation ->
            defineConfigurationsForCompilation(compilation)

            if (createDefaultSourceSets) {
                project.kotlinExtension.sourceSets.maybeCreate(compilation.defaultSourceSetName).also { sourceSet ->
                    compilation.source(sourceSet) // also adds dependencies, requires the configurations for target and source set to exist at this point
                }
            }

            if (compilation is KotlinCompilationWithResources) {
                configureResourceProcessing(compilation, project.files(Callable { compilation.allKotlinSourceSets.map { it.resources } }))
            }

            createLifecycleTask(compilation)
        }
    }

    protected fun configureResourceProcessing(
        compilation: KotlinCompilationWithResources<*>,
        resourceSet: FileCollection
    ) {
        val project = compilation.target.project

        val resourcesTask = project.tasks.maybeCreate(compilation.processResourcesTaskName, ProcessResources::class.java)
        resourcesTask.description = "Processes $resourceSet."
        DslObject(resourcesTask).conventionMapping.map("destinationDir") { project.file(compilation.output.resourcesDir) }
        resourcesTask.from(resourceSet)
    }

    protected fun createLifecycleTask(compilation: KotlinCompilation<*>) {
        val project = compilation.target.project

        compilation.output.classesDirs.from(project.files().builtBy(compilation.compileAllTaskName))

        project.tasks.create(compilation.compileAllTaskName).apply {
            group = LifecycleBasePlugin.BUILD_GROUP
            description = "Assembles outputs for compilation '${compilation.name}' of target '${compilation.target.name}'"
            dependsOn(compilation.compileKotlinTaskName)
            if (compilation is KotlinCompilationWithResources) {
                dependsOn(compilation.processResourcesTaskName)
            }
        }
    }

    protected open fun defineConfigurationsForTarget(target: KotlinTargetType) {
        val project = target.project

        val configurations = project.configurations

        val defaultConfiguration = configurations.maybeCreate(target.defaultConfigurationName).apply {
            setupAsLocalTargetSpecificConfigurationIfSupported(target)
        }

        val mainCompilation = target.compilations.maybeCreate(KotlinCompilation.MAIN_COMPILATION_NAME)

        val compileConfiguration = configurations.maybeCreate(mainCompilation.deprecatedCompileConfigurationName)
        val implementationConfiguration = configurations.maybeCreate(mainCompilation.implementationConfigurationName)

        val runtimeOnlyConfiguration = configurations.maybeCreate(mainCompilation.runtimeOnlyConfigurationName)

        val apiElementsConfiguration = configurations.maybeCreate(target.apiElementsConfigurationName).apply {
            description = "API elements for main."
            isVisible = false
            isCanBeResolved = false
            isCanBeConsumed = true
            attributes.attribute<Usage>(USAGE_ATTRIBUTE, KotlinUsages.producerApiUsage(target))
            extendsFrom(configurations.maybeCreate(mainCompilation.apiConfigurationName))
            if (mainCompilation is KotlinCompilationToRunnableFiles) {
                val runtimeConfiguration = configurations.maybeCreate(mainCompilation.deprecatedRuntimeConfigurationName)
                extendsFrom(runtimeConfiguration)
            }
            usesPlatformOf(target)
            setupAsPublicConfigurationIfSupported(target)
        }

        if (mainCompilation is KotlinCompilationToRunnableFiles<*>) {
            val runtimeElementsConfiguration = configurations.maybeCreate(target.runtimeElementsConfigurationName).apply {
                description = "Elements of runtime for main."
                isVisible = false
                isCanBeConsumed = true
                isCanBeResolved = false
                attributes.attribute<Usage>(USAGE_ATTRIBUTE, KotlinUsages.producerRuntimeUsage(target))
                val runtimeConfiguration = configurations.maybeCreate(mainCompilation.deprecatedRuntimeConfigurationName)
                extendsFrom(implementationConfiguration, runtimeOnlyConfiguration, runtimeConfiguration)
                usesPlatformOf(target)
                setupAsPublicConfigurationIfSupported(target)
            }
            defaultConfiguration.extendsFrom(runtimeElementsConfiguration)
        } else {
            defaultConfiguration.extendsFrom(apiElementsConfiguration)
        }

        if (createTestCompilation) {
            val testCompilation = target.compilations.getByName(KotlinCompilation.TEST_COMPILATION_NAME)
            val compileTestsConfiguration = configurations.maybeCreate(testCompilation.deprecatedCompileConfigurationName)
            val testImplementationConfiguration = configurations.maybeCreate(testCompilation.implementationConfigurationName)
            val testRuntimeOnlyConfiguration = configurations.maybeCreate(testCompilation.runtimeOnlyConfigurationName)

            compileTestsConfiguration.extendsFrom(compileConfiguration)
            testImplementationConfiguration.extendsFrom(implementationConfiguration)
            testRuntimeOnlyConfiguration.extendsFrom(runtimeOnlyConfiguration)

            if (mainCompilation is KotlinCompilationToRunnableFiles && testCompilation is KotlinCompilationToRunnableFiles) {
                val runtimeConfiguration = configurations.maybeCreate(mainCompilation.deprecatedRuntimeConfigurationName)
                val testRuntimeConfiguration = configurations.maybeCreate(testCompilation.deprecatedRuntimeConfigurationName)
                testRuntimeConfiguration.extendsFrom(runtimeConfiguration)
            }
        }
    }

    protected fun configureBuild(target: KotlinTargetType) {
        val project = target.project

        val buildNeeded = project.tasks.getByName(JavaBasePlugin.BUILD_NEEDED_TASK_NAME)
        val buildDependent = project.tasks.getByName(JavaBasePlugin.BUILD_DEPENDENTS_TASK_NAME)

        if (createTestCompilation) {
            val testCompilation = target.compilations.getByName(KotlinCompilation.TEST_COMPILATION_NAME)
            if (testCompilation is KotlinCompilationToRunnableFiles) {
                addDependsOnTaskInOtherProjects(buildNeeded, true, testCompilation.deprecatedRuntimeConfigurationName)
                addDependsOnTaskInOtherProjects(buildDependent, false, testCompilation.deprecatedRuntimeConfigurationName)
            }
        }
    }

    private fun addDependsOnTaskInOtherProjects(task: Task, useDependedOn: Boolean, configurationName: String) {
        val project = task.project
        val configuration = project.configurations.getByName(configurationName)
        task.dependsOn(configuration.getTaskDependencyFromProjectDependency(useDependedOn, task.name))
    }

    companion object {
        const val testTaskNameSuffix = "test"

        fun defineConfigurationsForCompilation(
            compilation: KotlinCompilation<*>
        ) {
            val target = compilation.target
            val configurations = target.project.configurations

            val compileConfiguration = configurations.maybeCreate(compilation.deprecatedCompileConfigurationName).apply {
                setupAsLocalTargetSpecificConfigurationIfSupported(target)
                isVisible = false
                isCanBeResolved = true // Needed for IDE import
                description = "Dependencies for $compilation (deprecated, use '${compilation.implementationConfigurationName} ' instead)."
            }

            val apiConfiguration = configurations.maybeCreate(compilation.apiConfigurationName).apply {
                extendsFrom(compileConfiguration)
                isVisible = false
                isCanBeConsumed = false
                isCanBeResolved = false
                description = "API dependencies for $compilation."
            }

            val implementationConfiguration = configurations.maybeCreate(compilation.implementationConfigurationName).apply {
                extendsFrom(compileConfiguration, apiConfiguration)
                isVisible = false
                isCanBeConsumed = false
                isCanBeResolved = false
                description = "Implementation only dependencies for $compilation."
            }

            val compileOnlyConfiguration = configurations.maybeCreate(compilation.compileOnlyConfigurationName).apply {
                setupAsLocalTargetSpecificConfigurationIfSupported(target)
                isVisible = false
                isCanBeResolved = true // Needed for IDE import
                description = "Compile only dependencies for $compilation."
            }

            val compileClasspathConfiguration = configurations.maybeCreate(compilation.compileDependencyConfigurationName).apply {
                extendsFrom(compileOnlyConfiguration, implementationConfiguration)
                usesPlatformOf(target)
                isVisible = false
                isCanBeConsumed = false
                attributes.attribute(USAGE_ATTRIBUTE, KotlinUsages.consumerApiUsage(compilation.target))
                description = "Compile classpath for $compilation."
            }

            if (compilation is KotlinCompilationToRunnableFiles) {
                val runtimeConfiguration = configurations.maybeCreate(compilation.deprecatedRuntimeConfigurationName).apply {
                    setupAsLocalTargetSpecificConfigurationIfSupported(target)
                    extendsFrom(compileConfiguration)
                    isVisible = false
                    isCanBeResolved = true // Needed for IDE import
                    description =
                            "Runtime dependencies for $compilation (deprecated, use '${compilation.runtimeOnlyConfigurationName} ' instead)."
                }

                val runtimeOnlyConfiguration = configurations.maybeCreate(compilation.runtimeOnlyConfigurationName).apply {
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
                    attributes.attribute(USAGE_ATTRIBUTE, KotlinUsages.consumerRuntimeUsage(compilation.target))
                    description = "Runtime classpath of $compilation."
                }
            }
        }
    }
}

internal val KotlinCompilation<*>.deprecatedCompileConfigurationName: String
    get() = disambiguateName("compile")

internal val KotlinCompilationToRunnableFiles<*>.deprecatedRuntimeConfigurationName: String
    get() = disambiguateName("runtime")

internal val KotlinTarget.testTaskName: String
    get() = lowerCamelCaseName(targetName, AbstractKotlinTargetConfigurator.testTaskNameSuffix)

abstract class KotlinTargetConfigurator<KotlinCompilationType : KotlinCompilation<*>>(
    createDefaultSourceSets: Boolean,
    createTestCompilation: Boolean,
    val kotlinPluginVersion: String
) : AbstractKotlinTargetConfigurator<KotlinOnlyTarget<KotlinCompilationType>>(
    createDefaultSourceSets,
    createTestCompilation
) {
    internal abstract fun buildCompilationProcessor(compilation: KotlinCompilationType): KotlinSourceSetProcessor<*>

    override fun configureCompilations(platformTarget: KotlinOnlyTarget<KotlinCompilationType>) {
        super.configureCompilations(platformTarget)

        platformTarget.compilations.all { compilation ->
            buildCompilationProcessor(compilation).run()
            if (compilation.name == KotlinCompilation.MAIN_COMPILATION_NAME) {
                sourcesJarTask(compilation, platformTarget.targetName, platformTarget.targetName.toLowerCase())
            }
        }
    }

    override fun configureArchivesAndComponent(target: KotlinOnlyTarget<KotlinCompilationType>) {
        val project = target.project

        val mainCompilation = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)

        val jar = project.tasks.create(target.artifactsTaskName, Jar::class.java)
        jar.description = "Assembles a jar archive containing the main classes."
        jar.group = BasePlugin.BUILD_GROUP
        jar.from(mainCompilation.output.allOutputs)

        val apiElementsConfiguration = project.configurations.getByName(target.apiElementsConfigurationName)

        target.disambiguationClassifier?.let { jar.appendix = it.toLowerCase() }

        // Workaround: adding the artifact during configuration seems to interfere with the Java plugin, which results into missing
        // task dependency 'assemble -> jar' if the Java plugin is applied after this steps
        project.afterEvaluate {
            project.artifacts.add(Dependency.ARCHIVES_CONFIGURATION, jar) { jarArtifact ->
                jarArtifact.builtBy(jar)
                jarArtifact.type = ArtifactTypeDefinition.JAR_TYPE

                addJar(apiElementsConfiguration, jarArtifact)

                if (mainCompilation is KotlinCompilationToRunnableFiles<*>) {
                    val runtimeConfiguration = project.configurations.getByName(mainCompilation.deprecatedRuntimeConfigurationName)
                    val runtimeElementsConfiguration = project.configurations.getByName(target.runtimeElementsConfigurationName)
                    addJar(runtimeConfiguration, jarArtifact)
                    addJar(runtimeElementsConfiguration, jarArtifact)
                    // TODO Check Gradle's special split into variants for classes & resources -- do we need that too?
                }
            }
        }
    }

    override fun configureTest(target: KotlinOnlyTarget<KotlinCompilationType>) {
        val testCompilation = target.compilations.findByName(KotlinCompilation.TEST_COMPILATION_NAME) as? KotlinCompilationToRunnableFiles<*>
            ?: return // Otherwise, there is no runtime classpath

        target.project.tasks.create(lowerCamelCaseName(target.disambiguationClassifier, testTaskNameSuffix), Test::class.java).apply {
            project.afterEvaluate {
                // use afterEvaluate to override the JavaPlugin defaults for Test tasks
                conventionMapping.map("testClassesDirs") { testCompilation.output.classesDirs }
                conventionMapping.map("classpath") { testCompilation.runtimeDependencyFiles }
                description = "Runs the unit tests."
                group = JavaBasePlugin.VERIFICATION_GROUP
                target.project.tasks.findByName(JavaBasePlugin.CHECK_TASK_NAME)?.dependsOn(this@apply)
            }
        }
    }

    private fun addJar(configuration: Configuration, jarArtifact: PublishArtifact) {
        val publications = configuration.outgoing

        // Configure an implicit variant
        publications.artifacts.add(jarArtifact)
        publications.attributes.attribute(ArtifactAttributes.ARTIFACT_FORMAT, ArtifactTypeDefinition.JAR_TYPE)
    }
}

internal fun Project.usageByName(usageName: String): Usage =
    project.objects.named(Usage::class.java, usageName)

fun Configuration.usesPlatformOf(target: KotlinTarget): Configuration {
    attributes.attribute(KotlinPlatformType.attribute, target.platformType)
    // TODO: Provide an universal way to copy attributes from the target.
    if (target is KotlinNativeTarget) {
        attributes.attribute(KotlinNativeTarget.konanTargetAttribute, target.konanTarget.name)
    }
    return this
}