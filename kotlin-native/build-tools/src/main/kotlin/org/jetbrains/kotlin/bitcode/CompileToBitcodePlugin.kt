/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bitcode

import kotlinBuildProperties
import org.gradle.api.*
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationVariant
import org.gradle.api.attributes.Usage
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.ExecClang
import org.jetbrains.kotlin.cpp.*
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.SanitizerKind
import org.jetbrains.kotlin.konan.target.TargetDomainObjectContainer
import org.jetbrains.kotlin.konan.target.TargetWithSanitizer
import org.jetbrains.kotlin.testing.native.GoogleTestExtension
import org.jetbrains.kotlin.utils.capitalized
import java.time.Duration
import javax.inject.Inject

private fun String.snakeCaseToUpperCamelCase() = split('_').joinToString(separator = "") { it.capitalized }

private fun fullTaskName(name: String, targetName: String, sanitizer: SanitizerKind?) = "${targetName}${name.snakeCaseToUpperCamelCase()}${sanitizer.taskSuffix}"

private val SanitizerKind?.taskSuffix
    get() = when (this) {
        null -> ""
        SanitizerKind.ADDRESS -> "_ASAN"
        SanitizerKind.THREAD -> "_TSAN"
    }

private val SanitizerKind?.description
    get() = when (this) {
        null -> ""
        SanitizerKind.ADDRESS -> " with ASAN"
        SanitizerKind.THREAD -> " with TSAN"
    }

/**
 * Adds new object named [name] and configure it with [action] or return already existing object with this name.
 *
 * Similar to [NamedDomainObjectContainer.maybeCreate] but with [action] argument that will be applied only if
 * an object is being created.
 */
private fun <T> NamedDomainObjectContainer<T>.getOrCreate(name: String, action: Action<in T>): T = try {
    this.create(name, action)
} catch (e: InvalidUserDataException) {
    this.getByName(name)
}

private fun Project.compileBitcodeElements(sourceSet: String, action: Action<in Configuration>): Configuration = configurations.getOrCreate("compileBitcode${sourceSet.capitalized}Elements") {
    description = "LLVM bitcode of all defined modules ($sourceSet sources)"
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(CppUsage.LLVM_BITCODE))
    }
    action.execute(this)
}

private fun Project.compileBitcodeElements(sourceSet: String) = compileBitcodeElements(sourceSet) {}

private fun Project.moduleCompileBitcodeElements(moduleName: String, sourceSet: String, action: Action<in Configuration>): Configuration = configurations.getOrCreate("${moduleName}CompileBitcode${sourceSet.capitalized}Elements") {
    description = "LLVM bitcode of $moduleName module ($sourceSet sources)"
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(CppUsage.LLVM_BITCODE))
    }
    action.execute(this)
}

private fun Project.moduleCompileBitcodeElements(moduleName: String, sourceSet: String) = moduleCompileBitcodeElements(moduleName, sourceSet) {}

private fun Configuration.targetVariant(target: TargetWithSanitizer): ConfigurationVariant = outgoing.variants.getOrCreate("$target") {
    attributes {
        attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, target)
    }
}

private abstract class RunGTestSemaphore : BuildService<BuildServiceParameters.None>
private abstract class CompileTestsSemaphore : BuildService<BuildServiceParameters.None>

open class CompileToBitcodeExtension @Inject constructor(val project: Project) : TargetDomainObjectContainer<CompileToBitcodeExtension.Target>(project) {
    init {
        this.factory = { target ->
            project.objects.newInstance<Target>(this, target)
        }
    }

    /**
     * Outgoing configuration with `main` parts of all modules.
     */
    val compileBitcodeMainElements = project.compileBitcodeElements(MAIN_SOURCE_SET_NAME)

    /**
     * Outgoing configuration with `testFixtures` parts of all modules.
     */
    val compileBitcodeTestFixturesElements = project.compileBitcodeElements(TEST_FIXTURES_SOURCE_SET_NAME) {
        outgoing {
            capability(CppConsumerPlugin.testFixturesCapability(project))
        }
    }

    /**
     * Outgoing configuration with `test` parts of all modules.
     */
    val compileBitcodeTestElements = project.compileBitcodeElements(TEST_SOURCE_SET_NAME) {
        outgoing {
            capability(CppConsumerPlugin.testCapability(project))
        }
    }

    // TODO: These should be set by the plugin users.
    private val DEFAULT_CPP_FLAGS = listOfNotNull(
            "-gdwarf-2".takeIf { project.kotlinBuildProperties.getBoolean("kotlin.native.isNativeRuntimeDebugInfoEnabled", false) },
            "-std=c++17",
            "-Werror",
            "-O2",
            "-fno-aligned-allocation", // TODO: Remove when all targets support aligned allocation in C++ runtime.
            "-Wall",
            "-Wextra",
            "-Wno-unused-parameter",  // False positives with polymorphic functions.
    )

    private val targetList = with(project) {
        provider { (rootProject.project(":kotlin-native").property("targetList") as? List<*>)?.filterIsInstance<String>() ?: emptyList() } // TODO: Can we make it better?
    }

    private val allTestsTasks by lazy {
        val name = project.name.capitalized
        targetList.get().associateBy(keySelector = { it }, valueTransform = {
            project.tasks.register("${it}${name}Tests") {
                description = "Runs all $name tests for $it"
                group = VERIFICATION_TASK_GROUP
            }
        })
    }

    /**
     * A group of source files that will be compiled together.
     *
     * There are 3 well known source sets: `main`, `testFixtures` and `test`.
     */
    abstract class SourceSet @Inject constructor(
            private val owner: CompileToBitcodeExtension,
            private val module: Module,
            private val name: String,
            private val _target: TargetWithSanitizer,
    ) : Named {
        val target by _target::target
        val sanitizer by _target::sanitizer

        override fun getName() = name

        private val project by owner::project

        /**
         * Resulting single LLVM bitcode file.
         */
        abstract val outputFile: RegularFileProperty

        /**
         * Directory where LLVM bitcode files for each of the [inputFiles] is placed.
         */
        abstract val outputDirectory: DirectoryProperty

        /**
         * Header paths to use when compiling [inputFiles].
         */
        abstract val headersDirs: ConfigurableFileCollection

        /**
         * Source files to compile.
         */
        abstract val inputFiles: ConfigurableFileTree

        /**
         * Additional task dependencies.
         */
        abstract val dependencies: ListProperty<TaskProvider<*>>

        protected abstract val onlyIf: ListProperty<Spec<in SourceSet>>

        /**
         * Builds this source set only if [spec] is satisfied.
         */
        fun onlyIf(spec: Spec<in SourceSet>) {
            this.onlyIf.add(spec)
        }

        private val compilationDatabase = project.extensions.getByType<CompilationDatabaseExtension>()
        private val execClang = project.extensions.getByType<ExecClang>()

        /**
         * Compiles source files into bitcode files.
         */
        val compileTask = project.tasks.register<ClangFrontend>("clangFrontend${module.name.capitalized}${name.capitalized}${_target.toString().capitalized}").apply {
            configure {
                this.description = "Compiles '${module.name}' (${this@SourceSet.name} sources) to bitcode for $_target"
                this.outputDirectory.set(this@SourceSet.outputDirectory)
                this.targetName.set(target.name)
                this.compiler.set(module.compiler)
                this.arguments.set(module.compilerArgs)
                this.arguments.addAll(when (sanitizer) {
                    null -> emptyList()
                    SanitizerKind.ADDRESS -> listOf("-fsanitize=address")
                    SanitizerKind.THREAD -> listOf("-fsanitize=thread")
                })
                this.headersDirs.from(this@SourceSet.headersDirs)
                this.inputFiles.from(this@SourceSet.inputFiles.dir)
                this.inputFiles.setIncludes(this@SourceSet.inputFiles.includes)
                this.inputFiles.setExcludes(this@SourceSet.inputFiles.excludes)
                this.workingDirectory.set(module.compilerWorkingDirectory)
                // TODO: Should depend only on the toolchain needed to build for the _target
                dependsOn(":kotlin-native:dependencies:update")
                dependsOn(this@SourceSet.dependencies)
                onlyIf {
                    this@SourceSet.onlyIf.get().all { it.isSatisfiedBy(this@SourceSet) }
                }
            }
            compilationDatabase.target(_target) {
                entry {
                    val compileTask = this@apply.get()
                    val args = listOf(execClang.resolveExecutable(compileTask.compiler.get())) + compileTask.compilerFlags.get() + execClang.clangArgsForCppRuntime(target.name)
                    directory.set(compileTask.workingDirectory)
                    files.setFrom(compileTask.inputFiles)
                    arguments.set(args)
                    // Only the location of output file matters, compdb does not depend on the compilation result.
                    output.set(compileTask.outputDirectory.locationOnly.map { it.asFile.absolutePath })
                }
                task.configure {
                    // Compile task depends on the toolchain (including headers) and on the source code (e.g. googletest).
                    // compdb task should also have these dependencies. This way the generated database will point to the
                    // code that actually exists.
                    // TODO: Should depend only on the toolchain needed to build for the _target
                    dependsOn(":kotlin-native:dependencies:update")
                    dependsOn(this@SourceSet.dependencies)
                }
            }
        }

        /**
         * Links bitcode files together.
         */
        val task = project.tasks.register<LlvmLink>("llvmLink${module.name.capitalized}${name.capitalized}${_target.toString().capitalized}").apply {
            configure {
                this.description = "Link '${module.name}' bitcode files (${this@SourceSet.name} sources) into a single bitcode file for $_target"
                this.inputFiles.from(compileTask)
                this.outputFile.set(this@SourceSet.outputFile)
                this.arguments.set(module.linkerArgs)
                onlyIf {
                    this@SourceSet.onlyIf.get().all { it.isSatisfiedBy(this@SourceSet) }
                }
            }
            project.compileBitcodeElements(this@SourceSet.name).targetVariant(_target).artifact(this)
            project.moduleCompileBitcodeElements(module.name, this@SourceSet.name).targetVariant(_target).artifact(this)
        }
    }

    // TODO: Consider putting each module in a gradle project of its own. Current project should be used for grouping (i.e. reexporting all
    //       compileBitcodeMainElements from subprojects under a single umbrella configuration) and integration testing.
    abstract class Module @Inject constructor(
            private val owner: CompileToBitcodeExtension,
            private val name: String,
            private val _target: TargetWithSanitizer,
    ) : Named {
        /**
         * A container for [SourceSet].
         *
         * 3 source sets are well known: [main], [testFixtures] and [test].
         */
        abstract class SourceSets @Inject constructor(private val module: Module, private val container: ExtensiblePolymorphicDomainObjectContainer<SourceSet>) : NamedDomainObjectContainer<SourceSet> by container {
            private val project by module::project

            // googleTestExtension is only used if testFixtures or tests are used.
            private val googleTestExtension by lazy { project.extensions.getByType<GoogleTestExtension>() }

            /**
             * Get `main` source set if it was configured.
             */
            val main: Provider<SourceSet>
                get() = named(MAIN_SOURCE_SET_NAME)

            /**
             * Configure `main` source set. Used for main module sources. Included into `compileBitcodeMainElements` configuration.
             */
            fun main(action: Action<in SourceSet>): SourceSet = create(MAIN_SOURCE_SET_NAME) {
                this.inputFiles.include("**/*.cpp", "**/*.mm")
                this.inputFiles.exclude("**/*Test.cpp", "**/*TestSupport.cpp", "**/*Test.mm", "**/*TestSupport.mm")
                compileTask.configure {
                    this.group = BUILD_TASK_GROUP
                }
                task.configure {
                    this.group = BUILD_TASK_GROUP
                }
                action.execute(this)
            }

            /**
             * Get `testFixtures` source set if it was configured.
             */
            val testFixtures: Provider<SourceSet>
                get() = named(TEST_FIXTURES_SOURCE_SET_NAME)

            /**
             * Configure `testFixtures` source set. Used for testing API parts of module. Included into `compileBitcodeTestFixturesElements` configuration.
             */
            fun testFixtures(action: Action<in SourceSet>): SourceSet = create(TEST_FIXTURES_SOURCE_SET_NAME) {
                this.inputFiles.include("**/*TestSupport.cpp", "**/*TestSupport.mm")
                this.headersDirs.from(googleTestExtension.headersDirs)
                // TODO: Must generally depend on googletest module headers which must itself depend on sources being present.
                dependencies.add(project.tasks.named("downloadGoogleTest"))
                compileTask.configure {
                    this.group = VERIFICATION_BUILD_TASK_GROUP
                }
                task.configure {
                    this.group = VERIFICATION_BUILD_TASK_GROUP
                }
                action.execute(this)
            }

            /**
             * Get `test` source set if it was configured.
             */
            val test: Provider<SourceSet>
                get() = named(TEST_SOURCE_SET_NAME)

            /**
             * Configure `test` source set. Used for test files of module. Included into `compileBitcodeTestElements` configuration.
             */
            fun test(action: Action<in SourceSet>): SourceSet = create(TEST_SOURCE_SET_NAME) {
                this.inputFiles.include("**/*Test.cpp", "**/*Test.mm")
                this.headersDirs.from(googleTestExtension.headersDirs)
                // TODO: Must generally depend on googletest module headers which must itself depend on sources being present.
                dependencies.add(project.tasks.named("downloadGoogleTest"))
                compileTask.configure {
                    this.group = VERIFICATION_BUILD_TASK_GROUP
                }
                task.configure {
                    this.group = VERIFICATION_BUILD_TASK_GROUP
                }
                action.execute(this)
            }
        }

        val target by _target::target
        val sanitizer by _target::sanitizer

        override fun getName() = name

        private val project by owner::project

        /**
         * Outgoing configuration with `main` part of this module.
         */
        val compileBitcodeMainElements = project.moduleCompileBitcodeElements(name, MAIN_SOURCE_SET_NAME) {
            outgoing {
                capability(CppConsumerPlugin.moduleCapability(project, this@Module.name))
            }
        }

        /**
         * Outgoing configuration with `testFixtures` part of this module.
         */
        val compileBitcodeTestFixturesElements = project.moduleCompileBitcodeElements(name, TEST_FIXTURES_SOURCE_SET_NAME) {
            outgoing {
                capability(CppConsumerPlugin.moduleTestFixturesCapability(project, this@Module.name))
            }
        }

        /**
         * Outgoing configuration with `test` part of this module.
         */
        val compileBitcodeTestElements = project.moduleCompileBitcodeElements(name, TEST_SOURCE_SET_NAME) {
            outgoing {
                capability(CppConsumerPlugin.moduleTestCapability(project, this@Module.name))
            }
        }

        /**
         * Directory where module sources are located. By default `src/<module name>`.
         */
        abstract val srcRoot: DirectoryProperty

        // TODO: This is actually API dependency. Make it so.
        /**
         * Header directories to use for compilation of all [SourceSet]s.
         */
        abstract val headersDirs: ConfigurableFileCollection

        /**
         * Compiler to use. Either `clang` or `clang++`.
         */
        abstract val compiler: Property<String>

        /**
         * Extra arguments to `llvm-link`.
         */
        abstract val linkerArgs: ListProperty<String>

        /**
         * Extra arguments to [compiler].
         */
        abstract val compilerArgs: ListProperty<String>

        /**
         * Directory in which [compiler] will be executed. Important for macro evaluation like `__FILE__`.
         */
        abstract val compilerWorkingDirectory: DirectoryProperty

        /**
         * Extra tqsk dependencies to be used for all [SourceSet]s.
         */
        abstract val dependencies: ListProperty<TaskProvider<*>>
        protected abstract val onlyIf: ListProperty<Spec<in Module>>

        /**
         * Builds this module only if [spec] is satisfied.
         */
        fun onlyIf(spec: Spec<in Module>) {
            this.onlyIf.add(spec)
        }

        /**
         * Container for [SourceSet]s.
         */
        val sourceSets by lazy {
            project.objects.newInstance<SourceSets>(this, project.objects.polymorphicDomainObjectContainer(SourceSet::class.java).apply {
                registerFactory(SourceSet::class.java) {
                    project.objects.newInstance<SourceSet>(owner, this@Module, it, _target).apply {
                        this.outputFile.convention(project.layout.buildDirectory.file("bitcode/$name/$_target/${this@Module.name}.bc"))
                        this.outputDirectory.convention(project.layout.buildDirectory.dir("bitcode/$name/$_target/${this@Module.name}"))
                        this.inputFiles.from(this@Module.srcRoot.dir("cpp"))
                        this.headersDirs.setFrom(this@Module.headersDirs)
                        dependencies.set(this@Module.dependencies)
                        onlyIf {
                            this@Module.onlyIf.get().all { it.isSatisfiedBy(this@Module) }
                        }
                    }
                }
            })
        }

        /**
         * Container for [SourceSet]s.
         */
        fun sourceSets(action: Action<in SourceSets>) = sourceSets.apply {
            action.execute(this)
        }
    }

    abstract class TestsGroup @Inject constructor(
            private val _target: TargetWithSanitizer,
    ) {
        val target by _target::target
        val sanitizer by _target::sanitizer
        abstract val testedModules: ListProperty<String>
        abstract val testSupportModules: ListProperty<String>
        abstract val testLauncherModule: Property<String>
    }

    abstract class Target @Inject constructor(
            private val owner: CompileToBitcodeExtension,
            private val _target: TargetWithSanitizer,
    ) {
        val target by _target::target
        val sanitizer by _target::sanitizer

        private val project by owner::project

        // A shared service used to limit parallel execution of test binaries.
        private val runGTestSemaphore = project.gradle.sharedServices.registerIfAbsent("runGTestSemaphore", RunGTestSemaphore::class.java) {
            // Probably can be made configurable if test reporting moves away from simple gtest stdout dumping.
            maxParallelUsages.set(1)
        }

        private val compileTestsSemaphore = project.gradle.sharedServices.registerIfAbsent("compileTestsSemaphore", CompileTestsSemaphore::class.java) {
            // TODO: Make the default always null when tests compilation stops consuming so much memory.
            val defaultParallelism = if (project.kotlinBuildProperties.isTeamcityBuild) 2 else null
            val parallelism = project.kotlinBuildProperties.getOrNull("kotlin.native.runtimeTestsCompilationParallelism")?.toString()?.toInt() ?: defaultParallelism
            parallelism?.let {
                maxParallelUsages.set(it)
            }
        }

        private val modules: NamedDomainObjectContainer<Module> = project.objects.polymorphicDomainObjectContainer(Module::class.java).apply {
            registerFactory(Module::class.java) {
                project.objects.newInstance<Module>(owner, it, _target).apply {
                    this.srcRoot.convention(project.layout.projectDirectory.dir("src/$name"))
                    this.headersDirs.from(this.srcRoot.dir("cpp"))
                    this.compiler.convention("clang++")
                    this.compilerArgs.set(owner.DEFAULT_CPP_FLAGS)
                    this.compilerWorkingDirectory.set(project.layout.projectDirectory.dir("src"))
                }
            }
        }

        fun module(
                name: String,
                action: Action<in Module>,
        ): Module = modules.create(name) {
            action.execute(this)
        }

        fun module(name: String): Provider<Module> = modules.named(name)

        fun testsGroup(
                testTaskName: String,
                action: Action<in TestsGroup>,
        ) {
            val testsGroup = project.objects.newInstance(TestsGroup::class.java, _target).apply {
                testSupportModules.set(listOf("googletest", "googlemock"))
                testLauncherModule.convention("test_support")
                action.execute(this)
                testLauncherModule.finalizeValue()
            }
            val target = testsGroup.target
            val sanitizer = testsGroup.sanitizer
            val testName = fullTaskName(testTaskName, target.name, sanitizer)

            val testLauncherConfiguration = project.configurations.create("${testTaskName}${_target.toString().capitalized}TestLauncher") {
                isCanBeConsumed = false
                isCanBeResolved = true
                attributes {
                    attribute(CppUsage.USAGE_ATTRIBUTE, project.objects.named(CppUsage.LLVM_BITCODE))
                }
            }
            val testsGroupConfiguration = project.configurations.create("${testTaskName}${_target.toString().capitalized}") {
                isCanBeConsumed = false
                isCanBeResolved = true
                attributes {
                    attribute(CppUsage.USAGE_ATTRIBUTE, project.objects.named(CppUsage.LLVM_BITCODE))
                }
            }
            project.dependencies {
                testsGroup.testLauncherModule.get().let { moduleName ->
                    testLauncherConfiguration(module(project(project.path), moduleName))
                    testLauncherConfiguration(moduleTestFixtures(project(project.path), moduleName))
                }
                testsGroup.testedModules.get().forEach { moduleName ->
                    testsGroupConfiguration(module(project(project.path), moduleName))
                    testsGroupConfiguration(moduleTestFixtures(project(project.path), moduleName))
                    testsGroupConfiguration(moduleTest(project(project.path), moduleName))
                }
                testsGroup.testSupportModules.get().forEach { moduleName ->
                    testsGroupConfiguration(module(project(project.path), moduleName))
                    testsGroupConfiguration(moduleTestFixtures(project(project.path), moduleName))
                }
            }

            val compileTask = project.tasks.register<CompileToExecutable>("${testName}Compile") {
                description = "Compile tests group '$testTaskName' for $target${sanitizer.description}"
                group = VERIFICATION_BUILD_TASK_GROUP
                this.target.set(target)
                this.sanitizer.set(sanitizer)
                this.outputFile.set(project.layout.buildDirectory.file("bin/test/${target}/$testName.${target.family.exeSuffix}"))
                this.llvmLinkFirstStageOutputFile.set(project.layout.buildDirectory.file("bitcode/test/$target/$testName-firstStage.bc"))
                this.llvmLinkOutputFile.set(project.layout.buildDirectory.file("bitcode/test/$target/$testName.bc"))
                this.compilerOutputFile.set(project.layout.buildDirectory.file("obj/$target/$testName.o"))
                val allModules = listOf(testsGroup.testLauncherModule.get()) + testsGroup.testSupportModules.get() + testsGroup.testedModules.get()
                // TODO: Superwrong. Module should carry dependencies to system libraries that are passed to the linker.
                val mimallocEnabled = allModules.contains("mimalloc")
                this.mimallocEnabled.set(mimallocEnabled)
                val mainFileConfiguration = testLauncherConfiguration.incoming.artifactView {
                    attributes {
                        attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, _target)
                    }
                }.files
                // TODO: Check if this is still required.
                this.mainFile.set(mainFileConfiguration.singleFile)
                dependsOn(mainFileConfiguration)
                val inputFilesConfiguration = testsGroupConfiguration.incoming.artifactView {
                    attributes {
                        attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, _target)
                    }
                }.files
                this.inputFiles.from(inputFilesConfiguration)

                // Limit parallelism.
                usesService(compileTestsSemaphore)
            }

            val runTask = project.tasks.register<RunGTest>(testName) {
                description = "Runs tests group '$testTaskName' for $target${sanitizer.description}"
                group = VERIFICATION_TASK_GROUP
                this.testName.set(testName)
                executable.set(compileTask.flatMap { it.outputFile })
                dependsOn(compileTask)
                reportFileUnprocessed.set(project.layout.buildDirectory.file("testReports/$testName/report.xml"))
                reportFile.set(project.layout.buildDirectory.file("testReports/$testName/report-with-prefixes.xml"))
                filter.set(project.findProperty("gtest_filter") as? String)
                tsanSuppressionsFile.set(project.layout.projectDirectory.file("tsan_suppressions.txt"))
                this.target.set(target)
                this.executionTimeout.set(
                    (project.findProperty("gtest_timeout") as? String)?.let {
                        Duration.parse("PT${it}")
                    } ?: Duration.ofMinutes(30)) // The tests binaries are big.

                usesService(runGTestSemaphore)
            }

            owner.allTestsTasks[target.name]!!.configure {
                dependsOn(runTask)
            }

            // TODO: Support tsan natively on macOS arm64.
            if (target == KonanTarget.MACOS_X64 && sanitizer == SanitizerKind.THREAD) {
                owner.allTestsTasks[KonanTarget.MACOS_ARM64.name]!!.configure {
                    dependsOn(runTask)
                }
            }
        }
    }

    companion object {
        const val BUILD_TASK_GROUP = LifecycleBasePlugin.BUILD_GROUP
        const val VERIFICATION_TASK_GROUP = LifecycleBasePlugin.VERIFICATION_GROUP
        const val VERIFICATION_BUILD_TASK_GROUP = "verification build"

        const val MAIN_SOURCE_SET_NAME = "main"
        const val TEST_FIXTURES_SOURCE_SET_NAME = "testFixtures"
        const val TEST_SOURCE_SET_NAME = "test"
    }
}

/**
 * Compiling C and C++ modules into LLVM bitcode.
 *
 * Creates [CompileToBitcodeExtension] extension named `bitcode`.
 *
 * Creates the following [configurations][org.gradle.api.artifacts.Configuration]:
 * * `compileBitcode{sourceSet}Elements` - like `apiElements` (sort of) from java plugin, or `{variant}LinkElements` from C++ plugin.
 *    Contains bitcode produced from `sourceSet` sources of all defined modules.
 * * `{module}CompileBitcode{sourceSet}Elements` - like `compileBitcode{sourceSet}Elements` but for a single `module`.
 *
 * Each of the defined configuration has [Usage attribute][Usage] set to [CppUsage.LLVM_BITCODE].
 * Each `*Elements` configuration has variants with [TargetWithSanitizer.TARGET_ATTRIBUTE] values.
 *
 * To depend on a specific module, use [module][org.jetbrains.kotlin.cpp.module] and [moduleTestFixtures][org.jetbrains.kotlin.cpp.moduleTestFixtures].
 *
 * @see CompileToBitcodeExtension extension that this plugin creates.
 */
open class CompileToBitcodePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.apply<CppConsumerPlugin>()
        project.apply<CompilationDatabasePlugin>()
        project.apply<GitClangFormatPlugin>()
        project.extensions.create<CompileToBitcodeExtension>("bitcode", project)
    }
}
