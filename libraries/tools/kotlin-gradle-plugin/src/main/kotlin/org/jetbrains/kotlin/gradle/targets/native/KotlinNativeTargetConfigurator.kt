/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.internal.artifacts.ArtifactAttributes
import org.gradle.api.internal.plugins.DefaultArtifactPublicationSet
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.TEST_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.KOTLIN_NATIVE_IGNORE_INCORRECT_DEPENDENCIES
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.targets.metadata.isKotlinGranularMetadataEnabled
import org.jetbrains.kotlin.gradle.targets.native.*
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeHostTest
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.testing.internal.configureConventions
import org.jetbrains.kotlin.gradle.testing.internal.kotlinTestRegistry
import org.jetbrains.kotlin.gradle.testing.testTaskName
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

open class KotlinNativeTargetConfigurator<T : KotlinNativeTarget>(
    private val kotlinPluginVersion: String
) : AbstractKotlinTargetConfigurator<T>(
    createDefaultSourceSets = true,
    createTestCompilation = true
) {
    private fun Project.klibOutputDirectory(
        compilation: AbstractKotlinNativeCompilation
    ): File {
        val targetSubDirectory = compilation.target.disambiguationClassifier?.let { "$it/" }.orEmpty()
        return buildDir.resolve("classes/kotlin/$targetSubDirectory${compilation.name}")
    }

    private fun addCompilerPlugins(compilation: AbstractKotlinNativeCompilation) {
        val project = compilation.target.project

        project.whenEvaluated {
            SubpluginEnvironment
                .loadSubplugins(project, kotlinPluginVersion)
                .addSubpluginOptions(project, compilation)

            compilation.compileKotlinTaskProvider.configure {
                it.compilerPluginClasspath = project.configurations.getByName(NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME)
            }
        }
    }

    // region Artifact creation.
    private fun Project.createKlibArtifact(
        compilation: KotlinNativeCompilation,
        artifactFile: Provider<File>,
        classifier: String?,
        producingTask: TaskProvider<*>,
        copy: Boolean = false
    ) {
        if (!compilation.konanTarget.enabledOnCurrentHost) {
            return
        }

        val apiElements = configurations.getByName(compilation.target.apiElementsConfigurationName)

        val realProducingTask: TaskProvider<*>
        // TODO: Someone remove this HACK PLEASE!
        val realArtifactFile = if (copy) {
            realProducingTask = project.registerTask<Copy>("copy${producingTask.name.capitalize()}") {
                val targetSubDirectory = compilation.target.disambiguationClassifier?.let { "$it/" }.orEmpty()
                it.destinationDir = project.buildDir.resolve("libs/$targetSubDirectory${compilation.name}")
                it.from(artifactFile)
                it.dependsOn(producingTask)
            }
            realProducingTask.map { (it as Copy).destinationDir.resolve(artifactFile.get().name) }
        } else {
            realProducingTask = producingTask
            artifactFile
        }

        with(apiElements) {
            val klibArtifact = project.artifacts.add(apiElements.name, realArtifactFile) { artifact ->
                artifact.name = compilation.name
                artifact.extension = "klib"
                artifact.type = "klib"
                artifact.classifier = classifier
                artifact.builtBy(realProducingTask)
            }
            project.extensions.getByType(DefaultArtifactPublicationSet::class.java).addCandidate(klibArtifact)
            artifacts.add(klibArtifact)
            attributes.attribute(ArtifactAttributes.ARTIFACT_FORMAT, NativeArtifactFormat.KLIB)
        }
    }

    private fun Project.createRegularKlibArtifact(
        compilation: KotlinNativeCompilation,
        compileTask: TaskProvider<out KotlinNativeCompile>
    ) = createKlibArtifact(compilation, compileTask.map { it.outputFile.get() }, null, compileTask)

    private fun Project.createCInteropKlibArtifact(
        interop: DefaultCInteropSettings,
        interopTask: TaskProvider<out CInteropProcess>
    ) = createKlibArtifact(interop.compilation, interopTask.map { it.outputFile }, "cinterop-${interop.name}", interopTask, copy = true)
    // endregion.

    // region Task creation.
    private fun Project.createLinkTask(binary: NativeBinary) {
        val result = registerTask<KotlinNativeLink>(
            binary.linkTaskName
        ) {
            val target = binary.target
            it.binary = binary
            it.group = BasePlugin.BUILD_GROUP
            it.description = "Links ${binary.outputKind.description} '${binary.name}' for a target '${target.name}'."
            it.enabled = binary.konanTarget.enabledOnCurrentHost
            it.destinationDir = binary.outputDirectory
        }


        if (binary !is TestExecutable) {
            tasks.named(binary.compilation.target.artifactsTaskName).configure { it.dependsOn(result) }
            tasks.maybeCreate(LifecycleBasePlugin.ASSEMBLE_TASK_NAME).dependsOn(result)
        }

        if (binary is Framework) {
            createFrameworkArtifact(binary, result)
        }
    }

    private fun Project.createFrameworkArtifact(
        binary: Framework,
        linkTask: TaskProvider<KotlinNativeLink>
    ) {
        fun <T : Task> Configuration.configureConfiguration(taskProvider: TaskProvider<T>) {
            project.afterEvaluate {
                val task = taskProvider.get()
                val artifactFile = when (task) {
                    is FatFrameworkTask -> task.fatFrameworkDir
                    else -> binary.outputFile
                }
                val linkArtifact = project.artifacts.add(name, artifactFile) { artifact ->
                    artifact.name = name
                    artifact.extension = "framework"
                    artifact.type = "binary"
                    artifact.classifier = "framework"
                    artifact.builtBy(task)
                }
                project.extensions.getByType(org.gradle.api.internal.plugins.DefaultArtifactPublicationSet::class.java)
                    .addCandidate(linkArtifact)
                artifacts.add(linkArtifact)
                attributes.attribute(KotlinPlatformType.attribute, binary.target.platformType)
                attributes.attribute(
                    ArtifactAttributes.ARTIFACT_FORMAT,
                    NativeArtifactFormat.FRAMEWORK
                )
                attributes.attribute(
                    KotlinNativeTarget.kotlinNativeBuildTypeAttribute,
                    binary.buildType.name
                )
                if (attributes.getAttribute(Framework.frameworkTargets) == null) {
                    attributes.attribute(
                        Framework.frameworkTargets,
                        setOf(binary.target.konanTarget.name)
                    )
                }
                // capture type parameter T
                fun <T> copyAttribute(key: Attribute<T>, from: AttributeContainer, to: AttributeContainer) {
                    to.attribute(key, from.getAttribute(key)!!)
                }
                binary.attributes.keySet().filter { it != KotlinNativeTarget.konanTargetAttribute }.forEach {
                    copyAttribute(it, binary.attributes, this.attributes)
                }
            }
        }

        configurations.create(lowerCamelCaseName(binary.name, binary.target.name)) {
            it.isCanBeConsumed = true
            it.isCanBeResolved = false
            it.configureConfiguration(linkTask)
        }

        val fatFrameworkConfigurationName = lowerCamelCaseName(binary.name, binary.target.konanTarget.family.name.toLowerCase(), "fat")
        val fatFrameworkTaskName = "link${fatFrameworkConfigurationName.capitalize()}"

        val fatFrameworkTask = try {
            tasks.named(fatFrameworkTaskName, FatFrameworkTask::class.java)
        } catch (e: UnknownDomainObjectException) {
            tasks.register(fatFrameworkTaskName, FatFrameworkTask::class.java) {
                it.baseName = binary.baseName
                it.destinationDir = it.destinationDir.resolve(binary.buildType.name.toLowerCase())
            }
        }

        fatFrameworkTask.configure {
            try {
                it.from(binary)
            } catch (e: Exception) {
                logger.warn("Cannot add binary ${binary.name} dependency to fat framework", e)
            }
        }

        // maybeCreate is not used as it does not provide way to configure once
        val fatConfiguration =
            configurations.findByName(fatFrameworkConfigurationName) ?: configurations.create(fatFrameworkConfigurationName) {
                it.isCanBeConsumed = true
                it.isCanBeResolved = false
                it.configureConfiguration(fatFrameworkTask)
            }

        fatConfiguration.attributes.attribute(
            Framework.frameworkTargets,
            (fatConfiguration.attributes.getAttribute(Framework.frameworkTargets) ?: setOf<String>()) + binary.target.konanTarget.name
        )
    }

    private fun Project.createRunTask(binary: Executable) {
        val taskName = binary.runTaskName ?: return
        registerTask<Exec>(taskName) { exec ->
            exec.group = RUN_GROUP
            exec.description = "Executes Kotlin/Native executable ${binary.name} for target ${binary.target.name}"

            exec.enabled = binary.konanTarget.isCurrentHost

            exec.executable = binary.outputFile.absolutePath
            exec.workingDir = project.projectDir

            exec.onlyIf { binary.outputFile.exists() }
            exec.dependsOn(binary.linkTaskName)
        }
    }

    internal fun Project.createKlibCompilationTask(compilation: AbstractKotlinNativeCompilation): TaskProvider<KotlinNativeCompile> {
        val compileTaskProvider = registerTask<KotlinNativeCompile>(
            compilation.compileKotlinTaskName
        ) {
            it.compilation.set(compilation)
            it.group = BasePlugin.BUILD_GROUP
            it.description = "Compiles a klibrary from the '${compilation.name}' " +
                    "compilation for target '${compilation.platformType.name}'."
            it.enabled = compilation.konanTarget.enabledOnCurrentHost

            it.destinationDir = klibOutputDirectory(compilation)
        }


        compilation.output.addClassesDir {
            project.files(compileTaskProvider.map { it.outputFile })
        }


        project.tasks.getByName(compilation.compileAllTaskName).dependsOn(compileTaskProvider)

        if (compilation.isMain()) {
            compilation as? KotlinNativeCompilation ?: error("Main shared-Native compilation is not yet supported!")

            project.tasks.getByName(compilation.target.artifactsTaskName).apply {
                dependsOn(compileTaskProvider)
            }
            project.tasks.getByName(LifecycleBasePlugin.ASSEMBLE_TASK_NAME).apply {
                dependsOn(compileTaskProvider)
            }
            createRegularKlibArtifact(compilation, compileTaskProvider)
        }
        addCompilerPlugins(compilation)


        return compileTaskProvider
    }

    private fun Project.createCInteropTasks(compilation: KotlinNativeCompilation) {
        compilation.cinterops.all { interop ->
            val interopTask = registerTask<CInteropProcess>(interop.interopProcessingTaskName) {
                it.settings = interop
                it.destinationDir = provider { klibOutputDirectory(compilation) }
                it.group = INTEROP_GROUP
                it.description = "Generates Kotlin/Native interop library '${interop.name}' " +
                        "for compilation '${compilation.name}'" +
                        "of target '${it.konanTarget.name}'."
                it.enabled = compilation.konanTarget.enabledOnCurrentHost
            }

            val interopOutput = project.files(interopTask.map { it.outputFileProvider })
            with(compilation) {
                // Register the interop library as a dependency of the compilation to make IDE happy.
                project.dependencies.add(compileDependencyConfigurationName, interopOutput)
                if (isMain()) {
                    // Register the interop library as an outgoing klib to allow depending on projects with cinterops.
                    project.dependencies.add(target.apiElementsConfigurationName, interopOutput)
                    // Add the interop library in publication.
                    createCInteropKlibArtifact(interop, interopTask)
                    // We cannot add the interop library in an compilation output because in this case
                    // IDE doesn't see this library in module dependencies. So we have to manually add
                    // main interop libraries in dependencies of the default test compilation.
                    target.compilations.findByName(TEST_COMPILATION_NAME)?.let { testCompilation ->
                        project.dependencies.add(testCompilation.compileDependencyConfigurationName, interopOutput)
                        testCompilation.cinterops.all {
                            it.dependencyFiles += interopOutput
                        }
                    }
                }
            }
        }
    }
    // endregion.

    // region Configuration.
    override fun configurePlatformSpecificModel(target: T) {
        configureBinaries(target)
        configureFrameworkExport(target)
        configureCInterops(target)

        if (PropertiesProvider(target.project).ignoreIncorrectNativeDependencies != true) {
            warnAboutIncorrectDependencies(target)
        }
    }

    override fun configureArchivesAndComponent(target: T): Unit = with(target.project) {
        registerTask<DefaultTask>(target.artifactsTaskName) { }
        target.compilations.all {
            createKlibCompilationTask(it)
        }

        val apiElements = configurations.getByName(target.apiElementsConfigurationName)

        apiElements.outgoing.attributes.attribute(ArtifactAttributes.ARTIFACT_FORMAT, NativeArtifactFormat.KLIB)

        if (project.isKotlinGranularMetadataEnabled) {
            project.configurations.create(target.hostSpecificMetadataElementsConfigurationName) { configuration ->
                configuration.isCanBeConsumed = true
                configuration.isCanBeResolved = false

                configuration.extendsFrom(*apiElements.extendsFrom.toTypedArray())

                fun <T> copyAttribute(from: AttributeContainer, to: AttributeContainer, attribute: Attribute<T>) {
                    to.attribute(attribute, from.getAttribute(attribute)!!)
                }
                with(apiElements.attributes) { keySet().forEach { copyAttribute(this, configuration.attributes, it) } }
                configuration.attributes.attribute(USAGE_ATTRIBUTE, objects.named(Usage::class.java, KotlinUsages.KOTLIN_METADATA))
            }
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
            project.registerTask<DefaultTask>(it.binariesTaskName)
        }

        project.whenEvaluated {
            target.binaries.forEach { binary ->
                project.tasks.named(binary.compilation.binariesTaskName).configure { binariesTask ->
                    binariesTask.dependsOn(binary.linkTaskName)
                }
            }
        }

        /**
         * We create test binaries for all platforms but test runs only for:
         *  - host platforms: macosX64, linuxX64, mingwX64;
         *  - simulated platforms: iosX64, tvosX64, watchosX64.
         * See more in [KotlinNativeTargetWithTestsConfigurator] and its subclasses.
         */
        target.binaries.test(listOf(NativeBuildType.DEBUG)) { }
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

        target.binaries.withType(AbstractNativeLibrary::class.java).all { framework ->
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

    override fun defineConfigurationsForTarget(target: T) {
        super.defineConfigurationsForTarget(target)
        implementationToApiElements(target)
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
                warn(
                    """
                    Such dependencies are not applicable for Kotlin/Native, consider changing the dependency type to 'implementation' or 'api'.
                    To disable this warning, set the $KOTLIN_NATIVE_IGNORE_INCORRECT_DEPENDENCIES=true project property
                    """.trimIndent()
                )
            }
        }
    }
    // endregion.

    object NativeArtifactFormat {
        const val KLIB = "org.jetbrains.kotlin.klib"
        const val FRAMEWORK = "org.jetbrains.kotlin.framework"
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
                attributes.attribute(USAGE_ATTRIBUTE, KotlinUsages.consumerApiUsage(target))
                description = "Dependencies for cinterop '${cinterop.name}' (compilation '${compilation.name}')."
            }
        }
    }
}

abstract class KotlinNativeTargetWithTestsConfigurator<
        TargetType : KotlinNativeTargetWithTests<TestRunType>,
        TestRunType : KotlinNativeBinaryTestRun,
        TaskType : KotlinNativeTest>(
    kotlinPluginVersion: String
) : KotlinNativeTargetConfigurator<TargetType>(kotlinPluginVersion),
    KotlinTargetWithTestsConfigurator<TestRunType, TargetType> {

    abstract val testTaskClass: Class<TaskType>

    abstract fun isTestTaskEnabled(target: TargetType): Boolean

    protected open fun configureTestTask(target: TargetType, testTask: TaskType) {
        testTask.group = LifecycleBasePlugin.VERIFICATION_GROUP
        testTask.description = "Executes Kotlin/Native unit tests for target ${target.name}."
        testTask.targetName = target.name

        testTask.enabled = isTestTaskEnabled(target)

        testTask.workingDir = target.project.projectDir.absolutePath

        testTask.configureConventions()
    }

    protected open fun configureTestRun(target: TargetType, testRun: AbstractKotlinNativeTestRun<TaskType>) {
        with(testRun) {
            val project = target.project

            val testTaskOrProvider = project.registerTask(testTaskName, testTaskClass) { testTask ->
                configureTestTask(target, testTask)
            }

            executionTask = testTaskOrProvider

            setExecutionSourceFrom(target.binaries.getTest(NativeBuildType.DEBUG))

            project.kotlinTestRegistry.registerTestTask(testTaskOrProvider)
        }
    }
}

class KotlinNativeTargetWithHostTestsConfigurator(kotlinPluginVersion: String) :
    KotlinNativeTargetWithTestsConfigurator<
            KotlinNativeTargetWithHostTests,
            KotlinNativeHostTestRun,
            KotlinNativeHostTest>(
        kotlinPluginVersion
    ) {

    override val testTaskClass: Class<KotlinNativeHostTest>
        get() = KotlinNativeHostTest::class.java

    override val testRunClass: Class<KotlinNativeHostTestRun>
        get() = KotlinNativeHostTestRun::class.java

    override fun isTestTaskEnabled(target: KotlinNativeTargetWithHostTests): Boolean =
        target.konanTarget.isCurrentHost

    override fun createTestRun(
        name: String,
        target: KotlinNativeTargetWithHostTests
    ): KotlinNativeHostTestRun =
        DefaultHostTestRun(name, target).apply { configureTestRun(target, this) }
}

class KotlinNativeTargetWithSimulatorTestsConfigurator(kotlinPluginVersion: String) :
    KotlinNativeTargetWithTestsConfigurator<
            KotlinNativeTargetWithSimulatorTests,
            KotlinNativeSimulatorTestRun,
            KotlinNativeSimulatorTest>(
        kotlinPluginVersion
    ) {

    override val testTaskClass: Class<KotlinNativeSimulatorTest>
        get() = KotlinNativeSimulatorTest::class.java

    override val testRunClass: Class<KotlinNativeSimulatorTestRun>
        get() = KotlinNativeSimulatorTestRun::class.java

    override fun isTestTaskEnabled(target: KotlinNativeTargetWithSimulatorTests): Boolean =
        HostManager.hostIsMac

    override fun configureTestTask(target: KotlinNativeTargetWithSimulatorTests, testTask: KotlinNativeSimulatorTest) {
        super.configureTestTask(target, testTask)

        testTask.deviceId = when (target.konanTarget) {
            KonanTarget.IOS_X64 -> "iPhone 8"
            KonanTarget.WATCHOS_X86 -> "Apple Watch Series 5 - 44mm"
            KonanTarget.WATCHOS_X64 -> "Apple Watch Series 5 - 44mm"
            KonanTarget.TVOS_X64 -> "Apple TV"
            else -> error("Simulator tests are not supported for platform ${target.konanTarget.name}")
        }
    }

    override fun createTestRun(
        name: String,
        target: KotlinNativeTargetWithSimulatorTests
    ): KotlinNativeSimulatorTestRun =
        DefaultSimulatorTestRun(name, target).apply { configureTestRun(target, this) }
}
