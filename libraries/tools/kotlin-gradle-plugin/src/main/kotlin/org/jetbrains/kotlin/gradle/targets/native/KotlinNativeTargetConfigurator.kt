/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Category
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
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.TEST_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.KOTLIN_NATIVE_IGNORE_INCORRECT_DEPENDENCIES
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.registerEmbedAndSignAppleFrameworkTask
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinCompilationData
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleVariant
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinNativeCompilationData
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.isMainCompilationData
import org.jetbrains.kotlin.gradle.targets.metadata.isKotlinGranularMetadataEnabled
import org.jetbrains.kotlin.gradle.targets.native.*
import org.jetbrains.kotlin.gradle.targets.native.internal.commonizeCInteropTask
import org.jetbrains.kotlin.gradle.targets.native.internal.createCInteropApiElementsKlibArtifact
import org.jetbrains.kotlin.gradle.targets.native.internal.locateOrCreateCInteropApiElementsConfiguration
import org.jetbrains.kotlin.gradle.targets.native.internal.locateOrCreateCInteropDependencyConfiguration
import org.jetbrains.kotlin.gradle.targets.native.tasks.*
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.testing.internal.configureConventions
import org.jetbrains.kotlin.gradle.testing.internal.kotlinTestRegistry
import org.jetbrains.kotlin.gradle.testing.testTaskName
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

open class KotlinNativeTargetConfigurator<T : KotlinNativeTarget> : AbstractKotlinTargetConfigurator<T>(
    createDefaultSourceSets = true,
    createTestCompilation = true
) {

    // region Task creation.
    private fun Project.createLinkTask(binary: NativeBinary) {
        val result = registerTask<KotlinNativeLink>(
            binary.linkTaskName, listOf(binary)
        ) {
            val target = binary.target
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
                    is FatFrameworkTask -> task.fatFramework
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

        fun configureFatFramework() {
            val fatFrameworkConfigurationName = lowerCamelCaseName(binary.name, binary.target.konanTarget.family.name.toLowerCase(), "fat")
            val fatFrameworkTaskName = "link${fatFrameworkConfigurationName.capitalize()}"

            val fatFrameworkTask = if (fatFrameworkTaskName in tasks.names) {
                tasks.named(fatFrameworkTaskName, FatFrameworkTask::class.java)
            } else {
                tasks.register(fatFrameworkTaskName, FatFrameworkTask::class.java) {
                    it.baseName = binary.baseName
                    it.destinationDir = it.destinationDir.resolve(binary.buildType.name.toLowerCase())
                }
            }

            fatFrameworkTask.configure {
                try {
                    it.from(binary)
                } catch (e: Exception) {
                    logger.warn("Cannot add binary ${binary.name} dependency to default fat framework", e)
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

        configurations.create(lowerCamelCaseName(binary.name, binary.target.name)) {
            it.isCanBeConsumed = true
            it.isCanBeResolved = false
            it.configureConfiguration(linkTask)
        }

        if (FatFrameworkTask.isSupportedTarget(binary.target)) {
            configureFatFramework()
        }
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

    // FIXME support creating interop tasks for PM20
    private fun Project.createCInteropTasks(
        compilation: KotlinNativeCompilation,
        cinterops: NamedDomainObjectCollection<DefaultCInteropSettings>
    ) {
        cinterops.all { interop ->
            val interopTask = registerTask<CInteropProcess>(interop.interopProcessingTaskName, listOf(interop)) {
                it.destinationDir = provider { klibOutputDirectory(compilation).resolve("cinterop") }
                it.group = INTEROP_GROUP
                it.description = "Generates Kotlin/Native interop library '${interop.name}' " +
                        "for compilation '${compilation.compilationPurpose}'" +
                        "of target '${it.konanTarget.name}'."
                it.enabled = compilation.konanTarget.enabledOnCurrentHost
            }

            project.commonizeCInteropTask?.configure { commonizeCInteropTask ->
                commonizeCInteropTask.from((interopTask.get()))
            }

            val interopOutput = project.files(interopTask.map { it.outputFileProvider })
            with(compilation) {
                // Register the interop library as a dependency of the compilation to make IDE happy.
                project.dependencies.add(compileDependencyConfigurationName, interopOutput)
                if (isMain()) {
                    // Register the interop library as an outgoing klib to allow depending on projects with cinterops.
                    project.dependencies.add(target.apiElementsConfigurationName, interopOutput)

                    // Add interop library to special CInteropApiElements configuration
                    createCInteropApiElementsKlibArtifact(interop, interopTask)

                    // Add the interop library in publication.
                    createKlibArtifact(
                        compilation = interop.compilation,
                        artifactFile = interopTask.map { it.outputFile },
                        classifier = "cinterop-${interop.name}",
                        producingTask = interopTask,
                        copy = true
                    )


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

        if (target.konanTarget.family.isAppleFamily) {
            registerEmbedAndSignAppleFrameworkTasks(target)
        }

        if (PropertiesProvider(target.project).ignoreIncorrectNativeDependencies != true) {
            warnAboutIncorrectDependencies(target)
        }
    }

    override fun configureArchivesAndComponent(target: T): Unit = with(target.project) {
        registerTask<DefaultTask>(target.artifactsTaskName) { }
        target.compilations.all { createKlibCompilationTask(it) }

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
        locateOrCreateCInteropApiElementsConfiguration(target)
        target.compilations.all { compilation ->
            createCInteropTasks(compilation, compilation.cinterops)
            compilation.cinterops.all { cinterop ->
                cinterop.dependencyFiles += locateOrCreateCInteropDependencyConfiguration(compilation, cinterop, target)
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
                attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
            }
        }

        target.binaries.withType(AbstractNativeLibrary::class.java).all { framework ->
            project.configurations.maybeCreate(framework.exportConfigurationName).apply {
                isVisible = false
                isTransitive = false
                isCanBeConsumed = false
                isCanBeResolved = true
                usesPlatformOf(target)
                attributes.attribute(USAGE_ATTRIBUTE, KotlinUsages.consumerApiUsage(target))
                attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
                description = "Dependenceis to be exported in framework ${framework.name} for target ${target.targetName}"
            }
        }
    }

    private fun registerEmbedAndSignAppleFrameworkTasks(target: KotlinNativeTarget) {
        val project = target.project
        target.binaries.withType(Framework::class.java).all { framework ->
            project.registerEmbedAndSignAppleFrameworkTask(framework)
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

        internal fun createKlibCompilationTask(
            compilation: KotlinNativeCompilationData<*>
        ): TaskProvider<KotlinNativeCompile> {
            val project = compilation.project
            val compileTaskProvider = project.registerTask<KotlinNativeCompile>(
                compilation.compileKotlinTaskName,
                listOf(compilation)
            ) {
                it.group = BasePlugin.BUILD_GROUP
                it.description = "Compiles a klibrary from the '${compilation.compilationPurpose}' " +
                        "compilation for target '${compilation.platformType.name}'."
                it.enabled = compilation.konanTarget.enabledOnCurrentHost

                it.destinationDirectory.set(project.klibOutputDirectory(compilation).resolve("klib"))
            }


            compilation.output.classesDirs.from(compileTaskProvider.flatMap { it.outputFile })

            project.project.tasks.getByName(compilation.compileAllTaskName).dependsOn(compileTaskProvider)

            if (compilation.isMainCompilationData()) {
                if (compilation is KotlinNativeCompilation) {
                    project.project.tasks.getByName(compilation.target.artifactsTaskName).apply {
                        dependsOn(compileTaskProvider)
                    }
                }

                project.project.tasks.getByName(LifecycleBasePlugin.ASSEMBLE_TASK_NAME).apply {
                    dependsOn(compileTaskProvider)
                }
            }
            val shouldAddCompileOutputsToElements = compilation.owner is KotlinGradleVariant || compilation.isMainCompilationData()
            if (shouldAddCompileOutputsToElements) {
                createRegularKlibArtifact(compilation, compileTaskProvider)
            }

            if (compilation is AbstractKotlinNativeCompilation) {
                // FIXME: support compiler plugins for PM20
                addCompilerPlugins(compilation)
            }

            return compileTaskProvider
        }

        private fun Project.klibOutputDirectory(
            compilation: KotlinCompilationData<*>
        ): File {
            val targetSubDirectory = compilation.compilationClassifier?.let { "$it/" }.orEmpty()
            return buildDir.resolve("classes/kotlin/$targetSubDirectory${compilation.compilationPurpose}")
        }

        private fun addCompilerPlugins(compilation: AbstractKotlinNativeCompilation) {
            val project = compilation.target.project

            project.whenEvaluated {
                SubpluginEnvironment
                    .loadSubplugins(project)
                    .addSubpluginOptions(project, compilation)

                compilation.compileKotlinTaskProvider.configure {
                    it.compilerPluginClasspath = project
                        .configurations
                        .getByName(compilation.pluginConfigurationName)
                }
            }
        }

        internal fun createRegularKlibArtifact(
            compilation: KotlinNativeCompilationData<*>,
            compileTask: TaskProvider<out KotlinNativeCompile>
        ) = createKlibArtifact(compilation, compileTask.map { it.outputFile.get() }, null, compileTask)

        private fun createKlibArtifact(
            compilation: KotlinNativeCompilationData<*>,
            artifactFile: Provider<File>,
            classifier: String?,
            producingTask: TaskProvider<*>,
            copy: Boolean = false
        ) {
            val project = compilation.project
            if (!compilation.konanTarget.enabledOnCurrentHost) {
                return
            }

            val apiElements = project.configurations.getByName(compilation.apiElementsConfigurationName)

            val realProducingTask: TaskProvider<*>
            // TODO: Someone remove this HACK PLEASE!
            val realArtifactFile = if (copy) {
                realProducingTask = project.project.registerTask<Copy>("copy${producingTask.name.capitalize()}") {
                    val targetSubDirectory = compilation.compilationClassifier?.let { "$it/" }.orEmpty()
                    it.destinationDir = project.project.buildDir.resolve("libs/$targetSubDirectory${compilation.compilationPurpose}")
                    it.from(artifactFile)
                    it.dependsOn(producingTask)
                }
                realProducingTask.map { (it as Copy).destinationDir.resolve(artifactFile.get().name) }
            } else {
                realProducingTask = producingTask
                artifactFile
            }

            with(apiElements) {
                val klibArtifact = project.project.artifacts.add(apiElements.name, realArtifactFile) { artifact ->
                    artifact.name = compilation.compilationPurpose
                    artifact.extension = "klib"
                    artifact.type = "klib"
                    artifact.classifier = classifier
                    artifact.builtBy(realProducingTask)
                }
                project.project.extensions.getByType(DefaultArtifactPublicationSet::class.java).addCandidate(klibArtifact)
                artifacts.add(klibArtifact)
                attributes.attribute(ArtifactAttributes.ARTIFACT_FORMAT, NativeArtifactFormat.KLIB)
            }
        }

        private val KotlinNativeCompilationData<*>.apiElementsConfigurationName: String
            get() = when (val dataOwner = owner) {
                is KotlinGradleVariant -> dataOwner.apiElementsConfigurationName
                is KotlinTarget -> dataOwner.apiElementsConfigurationName
                else -> error("unexpected owner of $this")
            }
    }
}

abstract class KotlinNativeTargetWithTestsConfigurator<
        TargetType : KotlinNativeTargetWithTests<TestRunType>,
        TestRunType : KotlinNativeBinaryTestRun,
        TaskType : KotlinNativeTest>(
) : KotlinNativeTargetConfigurator<TargetType>(),
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

class KotlinNativeTargetWithHostTestsConfigurator() :
    KotlinNativeTargetWithTestsConfigurator<
            KotlinNativeTargetWithHostTests,
            KotlinNativeHostTestRun,
            KotlinNativeHostTest>() {

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

class KotlinNativeTargetWithSimulatorTestsConfigurator :
    KotlinNativeTargetWithTestsConfigurator<
            KotlinNativeTargetWithSimulatorTests,
            KotlinNativeSimulatorTestRun,
            KotlinNativeSimulatorTest>() {

    override val testTaskClass: Class<KotlinNativeSimulatorTest>
        get() = KotlinNativeSimulatorTest::class.java

    override val testRunClass: Class<KotlinNativeSimulatorTestRun>
        get() = KotlinNativeSimulatorTestRun::class.java

    override fun isTestTaskEnabled(target: KotlinNativeTargetWithSimulatorTests): Boolean =
        HostManager.hostIsMac && HostManager.host.architecture == target.konanTarget.architecture

    override fun configureTestTask(target: KotlinNativeTargetWithSimulatorTests, testTask: KotlinNativeSimulatorTest) {
        super.configureTestTask(target, testTask)

        testTask.deviceId = when (target.konanTarget) {
            KonanTarget.IOS_X64, KonanTarget.IOS_SIMULATOR_ARM64 -> "iPhone 12"
            KonanTarget.WATCHOS_X86, KonanTarget.WATCHOS_X64, KonanTarget.WATCHOS_SIMULATOR_ARM64 -> "Apple Watch Series 5 - 44mm"
            KonanTarget.TVOS_X64, KonanTarget.TVOS_SIMULATOR_ARM64 -> "Apple TV"
            else -> error("Simulator tests are not supported for platform ${target.konanTarget.name}")
        }
    }

    override fun createTestRun(
        name: String,
        target: KotlinNativeTargetWithSimulatorTests
    ): KotlinNativeSimulatorTestRun =
        DefaultSimulatorTestRun(name, target).apply { configureTestRun(target, this) }
}
