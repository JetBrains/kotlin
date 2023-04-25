/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.*
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.internal.artifactTypeAttribute
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
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
    fun configureSourceSet(target: KotlinTargetType)

    fun configurePlatformSpecificModel(target: KotlinTargetType) = Unit
}

abstract class AbstractKotlinTargetConfigurator<KotlinTargetType : KotlinTarget>(
    internal val createTestCompilation: Boolean
) : KotlinTargetConfigurator<KotlinTargetType> {

    protected open val runtimeIncludesCompilationOutputs = true

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
            setupCompilationDependencyFiles(it)
        }

        if (createTestCompilation) {
            target.compilations.create(KotlinCompilation.TEST_COMPILATION_NAME).apply {
                associateWith(main)

                if (runtimeIncludesCompilationOutputs && this is KotlinCompilationToRunnableFiles) {
                    // TODO: fix inconsistency? KT-27272
                    runtimeDependencyFiles += project.files(output.allOutputs)
                }
            }
        }
    }

    override fun configureSourceSet(target: KotlinTargetType) {
        target.compilations.all { compilation ->
            compilation.source(compilation.defaultSourceSet) // also adds dependencies, requires the configurations for target and source set to exist at this point
        }
    }

    override fun configureCompilationDefaults(target: KotlinTargetType) {
        val project = target.project

        target.compilations.all { compilation ->
            compilation.internal.processResourcesTaskName?.let { processResourcesTaskName ->
                configureResourceProcessing(
                    compilation, processResourcesTaskName, project.files(Callable { compilation.allKotlinSourceSets.map { it.resources } })
                )
            }

            createLifecycleTask(compilation)
        }
    }

    protected fun configureResourceProcessing(
        compilation: KotlinCompilation<*>,
        processResourcesTaskName: String,
        resourceSet: FileCollection
    ) {
        val project = compilation.target.project

        val resourcesDestinationDir = project.file(compilation.output.resourcesDir)
        val resourcesTask = project.locateOrRegisterTask<ProcessResources>(processResourcesTaskName) { resourcesTask ->
            resourcesTask.description = "Processes $resourceSet."
            resourcesTask.from(resourceSet)
            resourcesTask.into(resourcesDestinationDir)
        }

        compilation.output.resourcesDirProvider = resourcesTask.map { resourcesDestinationDir }
    }

    protected fun createLifecycleTask(compilation: KotlinCompilation<*>) {
        val project = compilation.target.project

        project.registerTask<DefaultTask>(compilation.compileAllTaskName) {
            it.group = LifecycleBasePlugin.BUILD_GROUP
            it.description = "Assembles outputs for compilation '${compilation.name}' of target '${compilation.target.name}'"
            it.inputs.files(Callable {
                // the task may not be registered at this point, reference it lazily
                compilation.compileKotlinTaskProvider.map { it.outputs.files }
            })

            if (compilation is KotlinJvmCompilation && (compilation.target as? KotlinJvmTarget)?.withJavaEnabled == true) {
                it.inputs.files({ compilation.compileJavaTaskProvider?.map { it.outputs.files } })
            }

            it.inputs.files(compilation.output.resourcesDirProvider)
        }
        compilation.output.classesDirs.from(project.files().builtBy(compilation.compileAllTaskName))
    }

    override fun defineConfigurationsForTarget(target: KotlinTargetType) {
        val project = target.project

        val configurations = project.configurations

        val mainCompilation = target.compilations.maybeCreate(KotlinCompilation.MAIN_COMPILATION_NAME)

        val compileConfiguration = mainCompilation.internal.configurations.deprecatedCompileConfiguration
        val implementationConfiguration = configurations.maybeCreate(mainCompilation.implementationConfigurationName)

        val runtimeOnlyConfiguration = when (mainCompilation) {
            is KotlinCompilationToRunnableFiles<*> -> configurations.maybeCreate(mainCompilation.runtimeOnlyConfigurationName)
            else -> null
        }

        configurations.maybeCreate(target.apiElementsConfigurationName).apply {
            description = "API elements for main."
            isVisible = false
            isCanBeResolved = false
            isCanBeConsumed = true
            attributes.attribute(USAGE_ATTRIBUTE, KotlinUsages.producerApiUsage(target))
            attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
            extendsFrom(configurations.maybeCreate(mainCompilation.apiConfigurationName))
            if (mainCompilation is KotlinCompilationToRunnableFiles) {
                val runtimeConfiguration = mainCompilation.internal.configurations.deprecatedRuntimeConfiguration
                runtimeConfiguration?.let { extendsFrom(it) }
            }
            usesPlatformOf(target)
        }

        if (mainCompilation is KotlinCompilationToRunnableFiles<*>) {
            configurations.maybeCreate(target.runtimeElementsConfigurationName).apply {
                description = "Elements of runtime for main."
                isVisible = false
                isCanBeConsumed = true
                isCanBeResolved = false
                attributes.attribute(USAGE_ATTRIBUTE, KotlinUsages.producerRuntimeUsage(target))
                attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
                val runtimeConfiguration = mainCompilation.internal.configurations.deprecatedRuntimeConfiguration
                extendsFrom(implementationConfiguration)
                if (runtimeOnlyConfiguration != null)
                    extendsFrom(runtimeOnlyConfiguration)
                runtimeConfiguration?.let { extendsFrom(it) }
                usesPlatformOf(target)
            }
        }

        configurations.maybeCreate(target.sourcesElementsConfigurationName).apply {
            description = "Source files of main compilation of ${target.name}."
            isVisible = false
            isCanBeResolved = false
            isCanBeConsumed = true
            configureSourcesPublicationAttributes(target)
            project.whenEvaluated { isCanBeConsumed = target.internal.isSourcesPublishable }
        }

        if (createTestCompilation) {
            val testCompilation = target.compilations.getByName(KotlinCompilation.TEST_COMPILATION_NAME)
            val compileTestsConfiguration = testCompilation.internal.configurations.deprecatedCompileConfiguration
            val testImplementationConfiguration = configurations.maybeCreate(testCompilation.implementationConfigurationName)
            val testRuntimeOnlyConfiguration = when (testCompilation) {
                is KotlinCompilationToRunnableFiles<*> -> configurations.maybeCreate(testCompilation.runtimeOnlyConfigurationName)
                else -> null
            }

            compileConfiguration?.let { compileTestsConfiguration?.extendsFrom(it) }
            testImplementationConfiguration.extendsFrom(implementationConfiguration)
            testRuntimeOnlyConfiguration?.extendsFrom(runtimeOnlyConfiguration)

            if (mainCompilation is KotlinCompilationToRunnableFiles && testCompilation is KotlinCompilationToRunnableFiles) {
                val runtimeConfiguration = mainCompilation.internal.configurations.deprecatedRuntimeConfiguration
                val testRuntimeConfiguration = testCompilation.internal.configurations.deprecatedRuntimeConfiguration
                runtimeConfiguration?.let { testRuntimeConfiguration?.extendsFrom(it) }
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
                addDependsOnTaskInOtherProjects(project, buildNeeded, true, testCompilation.runtimeDependencyConfigurationName)
                addDependsOnTaskInOtherProjects(project, buildDependent, false, testCompilation.runtimeDependencyConfigurationName)
            }
        }
    }

    private fun addDependsOnTaskInOtherProjects(
        project: Project,
        taskProvider: TaskProvider<*>,
        useDependedOn: Boolean,
        configurationName: String
    ) {
        val configuration = project.configurations.getByName(configurationName)
        taskProvider.configure { task ->
            task.dependsOn(configuration.getTaskDependencyFromProjectDependency(useDependedOn, taskProvider.name))
        }
    }

    companion object {
        const val testTaskNameSuffix = "test"
        const val runTaskNameSuffix = "run"
    }
}

internal val KotlinTarget.testTaskName: String
    get() = lowerCamelCaseName(targetName, AbstractKotlinTargetConfigurator.testTaskNameSuffix)

abstract class KotlinOnlyTargetConfigurator<KotlinCompilationType : KotlinCompilation<*>, KotlinTargetType : KotlinOnlyTarget<KotlinCompilationType>>(
    createTestCompilation: Boolean
) : AbstractKotlinTargetConfigurator<KotlinTargetType>(createTestCompilation) {
    open val archiveType: String = ArtifactTypeDefinition.JAR_TYPE

    open val archiveTaskType: Class<out Zip>
        get() = Jar::class.java

    internal abstract fun buildCompilationProcessor(compilation: KotlinCompilationType): KotlinCompilationProcessor<*>

    override fun configureCompilations(target: KotlinTargetType) {
        super.configureCompilations(target)

        target.compilations.all { compilation ->
            buildCompilationProcessor(compilation).run()
            if (compilation.isMain()) {
                sourcesJarTask(compilation, target.targetName, target.targetName.toLowerCaseAsciiOnly())
            }
        }
    }

    /** The implementations are expected to create a [Zip] task under the name [KotlinTarget.artifactsTaskName] of the [target]. */
    protected open fun createArchiveTasks(target: KotlinTargetType): TaskProvider<out Zip> {
        return target.project.registerTask(
            target.artifactsTaskName,
            archiveTaskType
        ) {
            it.description = "Assembles an archive containing the main classes."
            it.group = BasePlugin.BUILD_GROUP
            it.from(target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME).output.allOutputs)
            it.isPreserveFileTimestamps = false
            it.isReproducibleFileOrder = true
        }
    }

    override fun configureArchivesAndComponent(target: KotlinTargetType) {
        val project = target.project

        val mainCompilation = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)

        val task = createArchiveTasks(target)

        target.disambiguationClassifier?.let { classifier ->
            task.configure { taskInstance ->
                taskInstance.archiveAppendix.set(classifier.toLowerCaseAsciiOnly())
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
                addJarIfNoArtifactsPresent(project, apiElementsConfiguration, jarArtifact)

                if (mainCompilation is KotlinCompilationToRunnableFiles<*>) {
                    val runtimeConfiguration = mainCompilation.internal.configurations.deprecatedRuntimeConfiguration
                    val runtimeElementsConfiguration = project.configurations.getByName(target.runtimeElementsConfigurationName)
                    runtimeConfiguration?.let { addJarIfNoArtifactsPresent(project, runtimeConfiguration, jarArtifact) }
                    addJarIfNoArtifactsPresent(project, runtimeElementsConfiguration, jarArtifact)
                }
            }
        }
    }

    private fun addJarIfNoArtifactsPresent(project: Project, configuration: Configuration, jarArtifact: PublishArtifact) {
        if (configuration.artifacts.isEmpty()) {
            val publications = configuration.outgoing

            // Configure an implicit variant
            publications.artifacts.add(jarArtifact)
            publications.attributes.attribute(project.artifactTypeAttribute, archiveType)
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
    objects.named(Usage::class.java, usageName)

internal fun Project.categoryByName(categoryName: String): Category =
    objects.named(Category::class.java, categoryName)

internal inline fun <reified T : Named> Project.attributeValueByName(attributeValueName: String): T =
    objects.named(T::class.java, attributeValueName)

fun Configuration.usesPlatformOf(target: KotlinTarget): Configuration {
    attributes.attribute(KotlinPlatformType.attribute, target.platformType)

    when (target.platformType) {
        KotlinPlatformType.jvm -> setJavaTargetEnvironmentAttributeIfSupported(target.project, "standard-jvm")
        KotlinPlatformType.androidJvm -> setJavaTargetEnvironmentAttributeIfSupported(target.project, "android")
        /**
         *  We set this attribute even for non-JVM-like targets (JS, Native) to avoid issues with Gradle variant-aware dependency resolution
         *  treating variants which don't have a particular attribute more preferable than those having it in those cases when Gradle failed
         *  to choose the best match by biggest compatible attributes set (by inclusion). Having an attribute not
         *  set on some variants might break if there appears one more third-party attribute such that:
         *      * it is not set on some variants;
         *      * according to the other attributes which are set on all variants, there are both compatible candidate variants
         *        which have this attribute and those which don't;
         *  Note that this attribute is not published to avoid issues with older Kotlin versions combined with newer Gradle
         *  see [org.jetbrains.kotlin.gradle.plugin.mpp.DefaultKotlinUsageContext.filterOutNonPublishableAttributes]
         */
        else -> setJavaTargetEnvironmentAttributeIfSupported(target.project, "non-jvm")
    }

    val publishJsCompilerAttribute = PropertiesProvider(target.project).publishJsCompilerAttribute

    if (publishJsCompilerAttribute && target is KotlinJsTarget) {
        attributes.attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.legacy)
    }

    if (publishJsCompilerAttribute && target is KotlinJsIrTarget && target.platformType == KotlinPlatformType.js) {
        attributes.attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir)
    }

    // TODO: Provide an universal way to copy attributes from the target.
    if (target is KotlinNativeTarget) {
        attributes.attribute(KotlinNativeTarget.konanTargetAttribute, target.konanTarget.name)
    }
    return this
}

private fun Configuration.setJavaTargetEnvironmentAttributeIfSupported(project: Project, value: String) {
    if (isGradleVersionAtLeast(7, 0)) {
        @Suppress("UNCHECKED_CAST")
        val attributeClass = Class.forName("org.gradle.api.attributes.java.TargetJvmEnvironment") as Class<out Named>

        @Suppress("UNCHECKED_CAST")
        val attributeKey = attributeClass.getField("TARGET_JVM_ENVIRONMENT_ATTRIBUTE").get(null) as Attribute<Named>

        val attributeValue = project.objects.named(attributeClass, value)
        attributes.attribute(attributeKey, attributeValue)
    }
}

internal val Project.commonKotlinPluginClasspath get() = configurations.getByName(PLUGIN_CLASSPATH_CONFIGURATION_NAME)
