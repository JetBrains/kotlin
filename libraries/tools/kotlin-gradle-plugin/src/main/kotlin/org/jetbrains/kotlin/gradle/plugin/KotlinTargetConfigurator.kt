/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
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
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.COMPILE
import org.jetbrains.kotlin.gradle.utils.RUNTIME
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import java.util.concurrent.Callable
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties

interface KotlinTargetConfigurator<KotlinTargetType : KotlinTarget> {
    fun configureTarget(
        target: KotlinTargetType
    ) {
        configureCompilationDefaults(target)
        configureCompilations(target)
        defineConfigurationsForTarget(target)
        configureArchivesAndComponent(target)
        configureSourceSet(target)
        configureBuild(target)
        configurePlatformSpecificModel(target)
    }

    fun configureCompilationDefaults(target: KotlinTargetType)
    fun configureCompilations(target: KotlinTargetType)
    fun defineConfigurationsForTarget(target: KotlinTargetType)
    fun configureArchivesAndComponent(target: KotlinTargetType)
    fun configureBuild(target: KotlinTargetType)
    fun configureSourceSet(target: KotlinTarget)

    fun configurePlatformSpecificModel(target: KotlinTargetType) = Unit
}

abstract class AbstractKotlinTargetConfigurator<KotlinTargetType : KotlinTarget>(
    protected val createDefaultSourceSets: Boolean,
    protected val createTestCompilation: Boolean
) : KotlinTargetConfigurator<KotlinTargetType> {

    private fun Project.registerOutputsForStaleOutputCleanup(kotlinCompilation: KotlinCompilation<*>) {
        tasks.withType<Delete>().named(LifecycleBasePlugin.CLEAN_TASK_NAME).configure { cleanTask ->
            cleanTask.delete(kotlinCompilation.output.allOutputs)
        }
    }

    protected open fun setupCompilationDependencyFiles(compilation: KotlinCompilation<KotlinCommonOptions>) {
        val project = compilation.target.project

        compilation.compileDependencyFiles = project.configurations.maybeCreate(compilation.compileDependencyConfigurationName)
        if (compilation is KotlinCompilationToRunnableFiles) {
            compilation.runtimeDependencyFiles = project.configurations.maybeCreate(compilation.runtimeDependencyConfigurationName)
        }
    }

    override fun configureCompilations(target: KotlinTargetType) {
        val project = target.project
        val main = target.compilations.create(KotlinCompilation.MAIN_COMPILATION_NAME)

        target.compilations.all {
            project.registerOutputsForStaleOutputCleanup(it)
            setupCompilationDependencyFiles(it)
        }

        if (createTestCompilation) {
            target.compilations.create(KotlinCompilation.TEST_COMPILATION_NAME).apply {
                associateWith(main)

                if (this is KotlinCompilationToRunnableFiles) {
                    // TODO: fix inconsistency? KT-27272
                    runtimeDependencyFiles += project.files(output.allOutputs)
                }
            }
        }
    }

    override fun configureSourceSet(target: KotlinTarget) {
        val project = target.project

        target.compilations.all { compilation ->
            if (createDefaultSourceSets) {
                project.kotlinExtension.sourceSets.maybeCreate(compilation.defaultSourceSetName).also { sourceSet ->
                    compilation.source(sourceSet) // also adds dependencies, requires the configurations for target and source set to exist at this point
                }
            }
        }
    }

    override fun configureCompilationDefaults(target: KotlinTargetType) {
        val project = target.project

        target.compilations.all { compilation ->
            defineConfigurationsForCompilation(compilation)

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

        project.locateOrRegisterTask<ProcessResources>(compilation.processResourcesTaskName) { resourcesTask ->
            resourcesTask.description = "Processes $resourceSet."
            resourcesTask.from(resourceSet)
            resourcesTask.into(project.file(compilation.output.resourcesDir))
        }
    }

    protected fun createLifecycleTask(compilation: KotlinCompilation<*>) {
        val project = compilation.target.project

        compilation.output.classesDirs.from(project.files().builtBy(compilation.compileAllTaskName))

        project.registerTask<DefaultTask>(compilation.compileAllTaskName) {
            it.group = LifecycleBasePlugin.BUILD_GROUP
            it.description = "Assembles outputs for compilation '${compilation.name}' of target '${compilation.target.name}'"
            it.dependsOn(compilation.compileKotlinTaskName)
            if (compilation is KotlinCompilationWithResources) {
                it.dependsOn(compilation.processResourcesTaskName)
            }
        }
    }

    override fun defineConfigurationsForTarget(target: KotlinTargetType) {
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

    @Deprecated("Remove when IR compiler to klib will not need transitive implementation dependencies")
    protected fun implementationToApiElements(target: KotlinTargetType) {
        val configurations = target.project.configurations

        // The configuration and the main compilation are created by the base class.
        val mainCompilation = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
        configurations.getByName(target.apiElementsConfigurationName).apply {
            //  K/N and K/JS IR compiler doesn't divide libraries into implementation and api ones. So we need to add implementation
            // dependencies into the outgoing configuration.
            extendsFrom(configurations.getByName(mainCompilation.implementationConfigurationName))
        }
    }

    override fun configureBuild(target: KotlinTargetType) {
        val project = target.project

        val buildNeeded = project.tasks.named(JavaBasePlugin.BUILD_NEEDED_TASK_NAME)
        val buildDependent = project.tasks.named(JavaBasePlugin.BUILD_DEPENDENTS_TASK_NAME)

        if (createTestCompilation) {
            val testCompilation = target.compilations.getByName(KotlinCompilation.TEST_COMPILATION_NAME)
            if (testCompilation is KotlinCompilationToRunnableFiles) {
                addDependsOnTaskInOtherProjects(project, buildNeeded, true, testCompilation.deprecatedRuntimeConfigurationName)
                addDependsOnTaskInOtherProjects(project, buildDependent, false, testCompilation.deprecatedRuntimeConfigurationName)
            }
        }
    }

    private fun addDependsOnTaskInOtherProjects(project: Project, taskProvider: TaskProvider<*>, useDependedOn: Boolean, configurationName: String) {
        val configuration = project.configurations.getByName(configurationName)
        taskProvider.configure { task ->
            task.dependsOn(configuration.getTaskDependencyFromProjectDependency(useDependedOn, taskProvider.name))
        }
    }

    companion object {
        const val testTaskNameSuffix = "test"
        const val runTaskNameSuffix = "run"

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
    get() = disambiguateName(COMPILE)

internal val KotlinCompilationToRunnableFiles<*>.deprecatedRuntimeConfigurationName: String
    get() = disambiguateName(RUNTIME)

internal val KotlinTarget.testTaskName: String
    get() = lowerCamelCaseName(targetName, AbstractKotlinTargetConfigurator.testTaskNameSuffix)

abstract class KotlinOnlyTargetConfigurator<KotlinCompilationType : KotlinCompilation<*>, KotlinTargetType : KotlinOnlyTarget<KotlinCompilationType>>(
    createDefaultSourceSets: Boolean,
    createTestCompilation: Boolean,
    val kotlinPluginVersion: String
) : AbstractKotlinTargetConfigurator<KotlinTargetType>(
    createDefaultSourceSets,
    createTestCompilation
) {
    open val archiveType: String = ArtifactTypeDefinition.JAR_TYPE

    internal abstract fun buildCompilationProcessor(compilation: KotlinCompilationType): KotlinCompilationProcessor<*>

    override fun configureCompilations(target: KotlinTargetType) {
        super.configureCompilations(target)

        target.compilations.all { compilation ->
            buildCompilationProcessor(compilation).run()
            if (compilation.isMain()) {
                sourcesJarTask(compilation, target.targetName, target.targetName.toLowerCase())
            }
        }
    }

    /** The implementations are expected to create a [Zip] task under the name [KotlinTarget.artifactsTaskName] of the [target]. */
    protected open fun createArchiveTasks(target: KotlinTargetType): TaskProvider<out Zip> {
        //TODO Change Jar on Zip
        return target.project.registerTask<Jar>(target.artifactsTaskName) {
            it.description = "Assembles an archive containing the main classes."
            it.group = BasePlugin.BUILD_GROUP
            it.from(target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME).output.allOutputs)
        }
    }

    override fun configureArchivesAndComponent(target: KotlinTargetType) {
        val project = target.project

        val mainCompilation = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)

        val task = createArchiveTasks(target)

        target.disambiguationClassifier?.let { classifier ->
            task.configure { taskInstance ->
                taskInstance.archiveAppendix.set(classifier.toLowerCase())
            }
        }

        // Workaround: adding the artifact during configuration seems to interfere with the Java plugin, which results into missing
        // task dependency 'assemble -> jar' if the Java plugin is applied after this steps
        project.afterEvaluate {
            project.artifacts.add(Dependency.ARCHIVES_CONFIGURATION, task) { jarArtifact ->
                jarArtifact.builtBy(task)
                jarArtifact.type = archiveType

                val apiElementsConfiguration = project.configurations.getByName(target.apiElementsConfigurationName)
                // If the target adds its own artifact to this configuration until this happens, don't add another one:
                addJarIfNoArtifactsPresent(apiElementsConfiguration, jarArtifact)

                if (mainCompilation is KotlinCompilationToRunnableFiles<*>) {
                    val runtimeConfiguration = project.configurations.getByName(mainCompilation.deprecatedRuntimeConfigurationName)
                    val runtimeElementsConfiguration = project.configurations.getByName(target.runtimeElementsConfigurationName)
                    addJarIfNoArtifactsPresent(runtimeConfiguration, jarArtifact)
                    addJarIfNoArtifactsPresent(runtimeElementsConfiguration, jarArtifact)
                }
            }
        }
    }

    private fun addJarIfNoArtifactsPresent(configuration: Configuration, jarArtifact: PublishArtifact) {
        if (configuration.artifacts.isEmpty()) {
            val publications = configuration.outgoing

            // Configure an implicit variant
            publications.artifacts.add(jarArtifact)
            publications.attributes.attribute(ArtifactAttributes.ARTIFACT_FORMAT, archiveType)
        }
    }
}

internal interface KotlinTargetWithTestsConfigurator<R : KotlinTargetTestRun<*>, T : KotlinTargetWithTests<*, R>>
    : KotlinTargetConfigurator<T> {

    override fun configureTarget(target: T) {
        super.configureTarget(target)
        configureTest(target)
    }

    val testRunClass: Class<R>

    fun createTestRun(name: String, target: T): R

    fun configureTest(target: T) {
        initializeTestRuns(target)
        target.testRuns.create(KotlinTargetWithTests.DEFAULT_TEST_RUN_NAME)
    }

    private fun initializeTestRuns(target: T) {
        val project = target.project

        val testRunsPropertyName = KotlinTargetWithTests<*, *>::testRuns.name
        val mutableProperty =
            target::class.memberProperties
                .find { it.name == testRunsPropertyName } as? KMutableProperty1<*, *>
                ?: error(
                    "The ${this::class.qualifiedName} implementation of ${KotlinTargetWithTests::class.qualifiedName} must " +
                            "override the $testRunsPropertyName property with a var."
                )

        val testRunsContainer = project.container(testRunClass) { testRunName -> createTestRun(testRunName, target) }

        @Suppress("UNCHECKED_CAST")
        (mutableProperty as KMutableProperty1<KotlinTargetWithTests<*, R>, NamedDomainObjectContainer<R>>)
            .set(target, testRunsContainer)

        (target as ExtensionAware).extensions.add(target::testRuns.name, testRunsContainer)
    }
}

internal fun Project.usageByName(usageName: String): Usage =
    project.objects.named(Usage::class.java, usageName)

fun Configuration.usesPlatformOf(target: KotlinTarget): Configuration {
    attributes.attribute(KotlinPlatformType.attribute, target.platformType)

    if (target is KotlinJsTarget) {
        attributes.attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.legacy)
    }

    if (target is KotlinJsIrTarget) {
        attributes.attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir)
    }

    // TODO: Provide an universal way to copy attributes from the target.
    if (target is KotlinNativeTarget) {
        attributes.attribute(KotlinNativeTarget.konanTargetAttribute, target.konanTarget.name)
    }
    return this
}