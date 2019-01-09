/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.*
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.artifacts.ArtifactAttributes
import org.gradle.api.internal.artifacts.publish.DefaultPublishArtifact
import org.gradle.api.internal.plugins.DefaultArtifactPublicationSet
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.Test
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeBinaryContainer
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.TEST_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinNativeCompile
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.gradle.utils.isGradleVersionAtLeast
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import java.io.File
import java.util.*
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

    protected fun configureCompilations(platformTarget: KotlinTargetType) {
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
            defineConfigurationsForCompilation(compilation, target, project.configurations)

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
            compilation: KotlinCompilation<*>,
            target: KotlinTarget,
            configurations: ConfigurationContainer
        ) {
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

open class KotlinTargetConfigurator<KotlinCompilationType : KotlinCompilation<*>>(
    createDefaultSourceSets: Boolean,
    createTestCompilation: Boolean
) : AbstractKotlinTargetConfigurator<KotlinOnlyTarget<KotlinCompilationType>>(
    createDefaultSourceSets,
    createTestCompilation
) {

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

        target.project.tasks.create(lowerCamelCaseName(target.targetName, testTaskNameSuffix), Test::class.java).apply {
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


open class KotlinNativeTargetConfigurator(
    private val kotlinPluginVersion: String
) : AbstractKotlinTargetConfigurator<KotlinNativeTarget>(
    createDefaultSourceSets = true,
    createTestCompilation = true
) {
    private fun Project.klibOutputDirectory(
        compilation: KotlinNativeCompilation
    ): File {
        val targetSubDirectory = compilation.target.disambiguationClassifier?.let { "$it/" }.orEmpty()
        return buildDir.resolve("classes/kotlin/$targetSubDirectory${compilation.name}")
    }

    private fun AbstractKotlinNativeCompile.addCompilerPlugins() {
        SubpluginEnvironment
            .loadSubplugins(project, kotlinPluginVersion)
            .addSubpluginOptions<CommonCompilerArguments>(project, this, compilerPluginOptions)
        compilerPluginClasspath = project.configurations.getByName(NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME)
    }

    // region Artifact creation.
    private fun Project.createKlibArtifact(
        compilation: KotlinNativeCompilation,
        artifactFile: File,
        classifier: String?,
        producingTask: Task,
        copy: Boolean = false
    ) {
        if (!compilation.target.konanTarget.enabledOnCurrentHost) {
            return
        }

        val apiElements = configurations.getByName(compilation.target.apiElementsConfigurationName)

        val realProducingTask: Task
        // TODO: Someone remove this HACK PLEASE!
        val realArtifactFile = if (copy) {
            realProducingTask = project.tasks.create("copy${producingTask.name.capitalize()}", Copy::class.java) {
                val targetSubDirectory = compilation.target.disambiguationClassifier?.let { "$it/" }.orEmpty()
                it.destinationDir = project.buildDir.resolve("libs/$targetSubDirectory${compilation.name}")
                it.from(artifactFile)
                it.dependsOn(producingTask)
            }
            realProducingTask.destinationDir.resolve(artifactFile.name)
        } else {
            realProducingTask = producingTask
            artifactFile
        }

        val klibArtifact = DefaultPublishArtifact(
            compilation.name,
            "klib",
            "klib",
            classifier,
            Date(),
            realArtifactFile,
            realProducingTask
        )
        project.extensions.getByType(DefaultArtifactPublicationSet::class.java).addCandidate(klibArtifact)

        with(apiElements.outgoing) {
            artifacts.add(klibArtifact)
            attributes.attribute(ArtifactAttributes.ARTIFACT_FORMAT, NativeArtifactFormat.KLIB)
        }
    }

    private fun Project.createRegularKlibArtifact(
        compilation: KotlinNativeCompilation,
        compileTask: KotlinNativeCompile
    ) = createKlibArtifact(compilation, compileTask.outputFile.get(), null, compileTask)

    private fun Project.createCInteropKlibArtifact(
        interop: DefaultCInteropSettings,
        interopTask: CInteropProcess
    ) = createKlibArtifact(interop.compilation, interopTask.outputFile, "cinterop-${interop.name}", interopTask, copy = true)
    // endregion.

    // region Task creation.
    private fun Project.createLinkTask(binary: NativeBinary) {
        tasks.create(
            binary.linkTaskName,
            KotlinNativeLink::class.java
        ).apply {
            val target = binary.target
            this.binary = binary
            group = BasePlugin.BUILD_GROUP
            description = "Links ${binary.outputKind.description} '${binary.name}' for a target '${target.name}'."
            enabled = target.konanTarget.enabledOnCurrentHost
            destinationDir = binary.outputDirectory
            addCompilerPlugins()

            tasks.maybeCreate(target.artifactsTaskName).dependsOn(this)
            tasks.maybeCreate(LifecycleBasePlugin.ASSEMBLE_TASK_NAME).dependsOn(this)
        }
    }

    private fun Project.createRunTask(binary: Executable) {
        val taskName = binary.runTaskName ?: return
        // TODO provide a special exec task for tests.
        tasks.create(taskName, Exec::class.java).apply {
            if (binary.isDefaultTestExecutable) {
                group = LifecycleBasePlugin.VERIFICATION_GROUP
                description = "Executes Kotlin/Native unit tests for target ${binary.target.name}."
                tasks.maybeCreate(LifecycleBasePlugin.CHECK_TASK_NAME).dependsOn(this)
                if (project.hasProperty("teamcity.version")) {
                    args("--ktest_logger=TEAMCITY")
                }
            } else {
                group = RUN_GROUP
                description = "Executes Kotlin/Native executable ${binary.name} for target ${binary.target.name}"
            }

            enabled = binary.target.konanTarget.isCurrentHost

            executable = binary.outputFile.absolutePath
            workingDir = project.projectDir

            onlyIf { binary.outputFile.exists() }
            dependsOn(binary.linkTaskName)
        }
    }

    private fun Project.createKlibCompilationTask(compilation: KotlinNativeCompilation) {
        val compileTask = tasks.create(
            compilation.compileKotlinTaskName,
            KotlinNativeCompile::class.java
        ).apply {
            this.compilation = compilation
            group = BasePlugin.BUILD_GROUP
            description = "Compiles a klibrary from the '${compilation.name}' " +
                    "compilation for target '${compilation.platformType.name}'."
            enabled = compilation.target.konanTarget.enabledOnCurrentHost

            destinationDir = klibOutputDirectory(compilation)
            addCompilerPlugins()
            compilation.output.tryAddClassesDir {
                project.files(this.outputFile).builtBy(this)
            }
        }

        project.tasks.getByName(compilation.compileAllTaskName).dependsOn(compileTask)

        if (compilation.compilationName == KotlinCompilation.MAIN_COMPILATION_NAME) {
            project.tasks.getByName(compilation.target.artifactsTaskName).apply {
                dependsOn(compileTask)
            }
            project.tasks.getByName(LifecycleBasePlugin.ASSEMBLE_TASK_NAME).apply {
                dependsOn(compileTask)
            }
            createRegularKlibArtifact(compilation, compileTask)
        }
    }

    private fun Project.createCInteropTasks(compilation: KotlinNativeCompilation) {
        compilation.cinterops.all { interop ->
            val interopTask = tasks.create(interop.interopProcessingTaskName, CInteropProcess::class.java).apply {
                settings = interop
                destinationDir = provider { klibOutputDirectory(compilation) }
                group = INTEROP_GROUP
                description = "Generates Kotlin/Native interop library '${interop.name}' " +
                        "for compilation '${compilation.name}'" +
                        "of target '${konanTarget.name}'."
                enabled = compilation.target.konanTarget.enabledOnCurrentHost

                val interopOutput = project.files(outputFileProvider).builtBy(this)
                with(compilation) {
                    project.dependencies.add(compileDependencyConfigurationName, interopOutput)
                    if (isMainCompilation) {
                        target.compilations.findByName(TEST_COMPILATION_NAME)?.let {
                            project.dependencies.add(it.compileDependencyConfigurationName, interopOutput)
                        }
                    }
                    project.dependencies.add(target.apiElementsConfigurationName, interopOutput)
                }
            }
            createCInteropKlibArtifact(interop, interopTask)
        }
    }
    // endregion.

    // region Configuration.
    override fun configureTarget(target: KotlinNativeTarget) {
        super.configureTarget(target)
        configureBinaries(target)
        configureFrameworkExport(target)
        configureCInterops(target)
        warnAboutIncorrectDependencies(target)
    }

    override fun configureArchivesAndComponent(target: KotlinNativeTarget): Unit = with(target.project) {
        tasks.create(target.artifactsTaskName)
        target.compilations.all {
            createKlibCompilationTask(it)
        }

        with(configurations.getByName(target.apiElementsConfigurationName)) {
            outgoing.attributes.attribute(ArtifactAttributes.ARTIFACT_FORMAT, NativeArtifactFormat.KLIB)
        }
    }

    override fun configureTest(target: KotlinNativeTarget) {
        target.binaries.defaultTestExecutable {
            compilation = target.compilations.maybeCreate(KotlinCompilation.TEST_COMPILATION_NAME)
            // Allow accessing the test binary using the old getters (e.g. compilations.test.getBinary('EXECUTABLE', 'DEBUG'))
            compilation.binaries[NativeOutputKind.EXECUTABLE to KotlinNativeBinaryContainer.DEFAULT_TEST_BUILD_TYPE] = this
        }
    }

    protected fun configureCInterops(target: KotlinNativeTarget): Unit = with(target.project) {
        target.compilations.all { compilation ->
            createCInteropTasks(compilation)
            compilation.cinterops.all {
                defineConfigurationsForCInterop(compilation, it, target, configurations)
            }
        }

        if (createTestCompilation) {
            val mainCompilation = target.compilations.getByName(MAIN_COMPILATION_NAME)
            target.compilations.findByName(TEST_COMPILATION_NAME)?.apply {
                cinterops.all {
                    it.dependencyFiles += mainCompilation.output.allOutputs
                }
            }
        }
    }

    protected fun configureBinaries(target: KotlinNativeTarget) {
        val project = target.project
        // Create link and run tasks.
        target.binaries.all {
            project.createLinkTask(it)
        }

        target.binaries.withType(Executable::class.java).all {
            project.createRunTask(it)
        }

        target.binaries.prefixGroups.all { prefixGroup ->
            val linkGroupTask = project.tasks.maybeCreate(prefixGroup.linkTaskName)
            prefixGroup.binaries.all {
                linkGroupTask.dependsOn(it.linkTaskName)
            }
        }

        // Create an aggregate link task for each compilation.
        target.compilations.all {
            project.tasks.create(it.binariesTaskName)
        }

        project.whenEvaluated {
            target.binaries.forEach {
                project.tasks.getByName(it.compilation.binariesTaskName).dependsOn(it.linkTaskName)
            }
        }

        // Create binaries for output kinds declared using the old DSL.
        project.whenEvaluated {
            target.compilations.all { compilation ->
                val binaries = target.binaries
                val konanTarget = compilation.target.konanTarget
                val name = compilation.name
                val buildTypes = compilation.buildTypes
                val availableOutputKinds = compilation.outputKinds.filter { it.availableFor(konanTarget) }

                val configure: NativeBinary.() -> Unit = {
                    this.compilation = compilation
                    linkerOpts.addAll(compilation.linkerOpts)
                    if (this is Executable) {
                        entryPoint = compilation.entryPoint
                    }
                    compilation.binaries[outputKind to buildType] = this
                }

                for (kind in availableOutputKinds) {
                    when (kind) {
                        NativeOutputKind.EXECUTABLE -> binaries.executable(name, buildTypes, configure)
                        NativeOutputKind.DYNAMIC -> binaries.sharedLib(name, buildTypes, configure)
                        NativeOutputKind.STATIC -> binaries.staticLib(name, buildTypes, configure)
                        NativeOutputKind.FRAMEWORK -> binaries.framework(name, buildTypes, configure)
                    }
                }
            }
        }
    }

    fun configureFrameworkExport(target: KotlinNativeTarget) {
        val project = target.project

        target.compilations.all {
            // Allow resolving api configurations directly to be able to check that
            // all exported dependency are also added in the corresponding api configurations.
            // The check is performed during a link task execution.
            project.configurations.maybeCreate(it.apiConfigurationName).apply {
                isCanBeResolved = true
                usesPlatformOf(target)
                attributes.attribute(USAGE_ATTRIBUTE, KotlinUsages.consumerApiUsage(target))
            }
        }

        target.binaries.withType(Framework::class.java).all { framework ->
            val exportConfiguration = project.configurations.maybeCreate(framework.exportConfigurationName).apply {
                isVisible = false
                isTransitive = false
                isCanBeConsumed = false
                isCanBeResolved = true
                usesPlatformOf(target)
                attributes.attribute(USAGE_ATTRIBUTE, KotlinUsages.consumerApiUsage(target))
                description = "Dependenceis to be exported in framework ${framework.name} for target ${target.targetName}"
            }
        }
    }

    override fun defineConfigurationsForTarget(target: KotlinNativeTarget) {
        super.defineConfigurationsForTarget(target)
        val configurations = target.project.configurations

        // The configuration and the main compilation are created by the base class.
        val mainCompilation = target.compilations.getByName(MAIN_COMPILATION_NAME)
        configurations.getByName(target.apiElementsConfigurationName).apply {
            //  K/N compiler doesn't divide libraries into implementation and api ones. So we need to add implementation
            // dependencies into the outgoing configuration.
            extendsFrom(configurations.getByName(mainCompilation.implementationConfigurationName))
        }
    }

    private fun warnAboutIncorrectDependencies(target: KotlinNativeTarget) = target.project.whenEvaluated {

        val compileOnlyDependencies = target.compilations.mapNotNull {
            val dependencies = configurations.getByName(it.compileOnlyConfigurationName).allDependencies
            if (dependencies.isNotEmpty()) {
                it to dependencies
            } else null
        }

        fun Dependency.stringCoordinates(): String = buildString {
            group?.let { append(it).append(':') }
            append(name)
            version?.let { append(':').append(it) }
        }

        if (compileOnlyDependencies.isNotEmpty()) {
            with(target.project.logger) {
                warn("A compileOnly dependency is used in the Kotlin/Native target '${target.name}':")
                compileOnlyDependencies.forEach {
                    warn(
                        """
                        Compilation: ${it.first.name}

                        Dependencies:
                        ${it.second.joinToString(separator = "\n") { it.stringCoordinates() }}

                    """.trimIndent()
                    )
                }
                warn("Such dependencies are not applicable for Kotlin/Native, consider changing the dependency type to 'implementation' or 'api'.")
            }
        }
    }
    // endregion.

    object NativeArtifactFormat {
        const val KLIB = "org.jetbrains.kotlin.klib"
    }

    companion object {
        const val INTEROP_GROUP = "interop"
        const val RUN_GROUP = "run"

        protected fun defineConfigurationsForCInterop(
            compilation: KotlinNativeCompilation,
            cinterop: CInteropSettings,
            target: KotlinTarget,
            configurations: ConfigurationContainer
        ) {
            val compileOnlyConfiguration = configurations.getByName(compilation.compileOnlyConfigurationName)
            val implementationConfiguration = configurations.getByName(compilation.implementationConfigurationName)

            cinterop.dependencyFiles = configurations.maybeCreate(cinterop.dependencyConfigurationName).apply {
                extendsFrom(compileOnlyConfiguration, implementationConfiguration)
                usesPlatformOf(target)
                isVisible = false
                isCanBeConsumed = false
                attributes.attribute(USAGE_ATTRIBUTE,  KotlinUsages.consumerApiUsage(target))
                description = "Dependencies for cinterop '${cinterop.name}' (compilation '${compilation.name}')."
            }
        }
    }
}

internal fun Project.usageByName(usageName: String): Usage =
    if (isGradleVersionAtLeast(4, 0)) {
        // `project.objects` is an API introduced in Gradle 4.0
        project.objects.named(Usage::class.java, usageName)
    } else {
        val usagesClass = Class.forName("org.gradle.api.internal.attributes.Usages")
        val usagesMethod = usagesClass.getMethod("usage", String::class.java)
        usagesMethod(null, usageName) as Usage
    }

fun Configuration.usesPlatformOf(target: KotlinTarget): Configuration {
    attributes.attribute(KotlinPlatformType.attribute, target.platformType)
    // TODO: Provide an universal way to copy attributes from the target.
    if (target is KotlinNativeTarget) {
        attributes.attribute(KotlinNativeTarget.konanTargetAttribute, target.konanTarget.name)
    }
    return this
}