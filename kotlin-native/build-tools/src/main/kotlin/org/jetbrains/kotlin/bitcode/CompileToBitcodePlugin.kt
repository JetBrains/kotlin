/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bitcode

import kotlinBuildProperties
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.kotlin.dsl.*
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.ExecClang
import org.jetbrains.kotlin.cpp.CompilationDatabaseExtension
import org.jetbrains.kotlin.cpp.CompilationDatabasePlugin
import org.jetbrains.kotlin.cpp.CompileToExecutable
import org.jetbrains.kotlin.cpp.RunGTest
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.PlatformManager
import org.jetbrains.kotlin.konan.target.SanitizerKind
import org.jetbrains.kotlin.konan.target.supportedSanitizers
import org.jetbrains.kotlin.testing.native.GoogleTestExtension
import org.jetbrains.kotlin.utils.Maybe
import org.jetbrains.kotlin.utils.asMaybe
import java.io.File
import javax.inject.Inject

private abstract class RunGTestSemaphore : BuildService<BuildServiceParameters.None>
private abstract class CompileTestsSemaphore : BuildService<BuildServiceParameters.None>

/**
 * A plugin creating extensions to compile
 */
open class CompileToBitcodePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.apply<CompilationDatabasePlugin>()
        target.extensions.create<CompileToBitcodeExtension>(EXTENSION_NAME, target)
    }

    companion object {
        const val EXTENSION_NAME = "bitcode"
    }
}

open class CompileToBitcodeExtension @Inject constructor(val project: Project) {
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

    private val compilationDatabase = project.extensions.getByType<CompilationDatabaseExtension>()
    private val execClang = project.extensions.getByType<ExecClang>()
    private val platformManager = project.extensions.getByType<PlatformManager>()

    // googleTestExtension is only used if testsGroup is used.
    private val googleTestExtension by lazy { project.extensions.getByType<GoogleTestExtension>() }

    // A shared service used to limit parallel execution of test binaries.
    private val runGTestSemaphore = project.gradle.sharedServices.registerIfAbsent("runGTestSemaphore", RunGTestSemaphore::class.java) {
        // Probably can be made configurable if test reporting moves away from simple gtest stdout dumping.
        maxParallelUsages.set(1)
    }

    // TODO: remove when tests compilation does not consume so much memory.
    private val compileTestsSemaphore = project.gradle.sharedServices.registerIfAbsent("compileTestsSemaphore", CompileTestsSemaphore::class.java) {
        maxParallelUsages.set(5)
    }

    private val targetList = with(project) {
        provider { (rootProject.project(":kotlin-native").property("targetList") as? List<*>)?.filterIsInstance<String>() ?: emptyList() } // TODO: Can we make it better?
    }

    private val allMainModulesTasks by lazy {
        val name = project.name.capitalized
        targetList.get().associateBy(keySelector = { it }, valueTransform = {
            project.tasks.register("${it}$name") {
                description = "Build all main modules of $name for $it"
                group = BUILD_TASK_GROUP
            }
        })
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

    private fun addToCompdb(compileTask: CompileToBitcode, konanTarget: KonanTarget) {
        // No need to generate compdb entry for sanitizers.
        if (compileTask.sanitizer != null) {
            return
        }
        compilationDatabase.target(konanTarget) {
            entry {
                val args = listOf(execClang.resolveExecutable(compileTask.compiler.get())) + compileTask.compilerFlags.get() + execClang.clangArgsForCppRuntime(konanTarget.name)
                directory.set(compileTask.outputDirectory)
                files.setFrom(compileTask.inputFiles)
                arguments.set(args)
                output.set(compileTask.outputFile.asFile.map { it.absolutePath })
            }
        }
    }

    fun module(name: String, srcRoot: File = project.file("src/$name"), outputGroup: String = "main", configurationBlock: CompileToBitcode.() -> Unit = {}) {
        targetList.get().forEach { targetName ->
            val target = platformManager.targetByName(targetName)
            val sanitizers: List<SanitizerKind?> = target.supportedSanitizers() + listOf(null)
            val allMainModulesTask = allMainModulesTasks[targetName]!!
            sanitizers.forEach { sanitizer ->
                val taskName = fullTaskName(name, targetName, sanitizer)
                val task = project.tasks.create(taskName, CompileToBitcode::class.java, target, sanitizer.asMaybe).apply {
                    this.moduleName.set(name)
                    this.outputFile.convention(moduleName.flatMap { project.layout.buildDirectory.file("bitcode/$outputGroup/$target${sanitizer.dirSuffix}/$it.bc") })
                    this.outputDirectory.convention(moduleName.flatMap { project.layout.buildDirectory.dir("bitcode/$outputGroup/$target${sanitizer.dirSuffix}/$it") })
                    this.compiler.convention("clang++")
                    this.compilerArgs.set(DEFAULT_CPP_FLAGS)
                    this.inputFiles.from(srcRoot.resolve("cpp"))
                    this.inputFiles.include("**/*.cpp", "**/*.mm")
                    this.inputFiles.exclude("**/*Test.cpp", "**/*TestSupport.cpp", "**/*Test.mm", "**/*TestSupport.mm")
                    this.headersDirs.from(this.inputFiles.dir)
                    when (outputGroup) {
                        "test" -> this.group = VERIFICATION_BUILD_TASK_GROUP
                        "main" -> this.group = BUILD_TASK_GROUP
                    }
                    this.description = "Compiles '$name' to bitcode for $targetName${sanitizer.description}"
                    dependsOn(":kotlin-native:dependencies:update")
                    configurationBlock()
                }
                addToCompdb(task, target)
                if (outputGroup == "main" && sanitizer == null) {
                    allMainModulesTask.configure {
                        dependsOn(taskName)
                    }
                }
            }
        }
    }

    abstract class TestsGroup @Inject constructor(
            val target: KonanTarget,
            private val _sanitizer: Maybe<SanitizerKind>,
    ) {
        val sanitizer
            get() = _sanitizer.orNull
        abstract val testedModules: ListProperty<String>
        abstract val testSupportModules: ListProperty<String>
        abstract val testLauncherModule: Property<String>
    }

    private fun createTestTask(
            testTaskName: String,
            testsGroup: TestsGroup,
    ) {
        val target = testsGroup.target
        val sanitizer = testsGroup.sanitizer
        val testName = fullTaskName(testTaskName, target.name, sanitizer)
        val testedTasks = testsGroup.testedModules.get().map {
            val name = fullTaskName(it, target.name, sanitizer)
            project.tasks.getByName(name) as CompileToBitcode
        }
        val compileToBitcodeTasks = testedTasks.mapNotNull {
            val name = "${it.name}TestBitcode"
            val task = project.tasks.findByName(name) as? CompileToBitcode
                    ?: project.tasks.create(name, CompileToBitcode::class.java, it.target, it.sanitizer.asMaybe).apply {
                        this.moduleName.set(it.moduleName)
                        this.outputFile.convention(moduleName.flatMap { project.layout.buildDirectory.file("bitcode/test/$target${sanitizer.dirSuffix}/${it}Tests.bc") })
                        this.outputDirectory.convention(moduleName.flatMap { project.layout.buildDirectory.dir("bitcode/test/$target${sanitizer.dirSuffix}/${it}Tests") })
                        this.compiler.convention("clang++")
                        this.compilerArgs.set(it.compilerArgs)
                        this.inputFiles.from(it.inputFiles.dir)
                        this.inputFiles.include("**/*Test.cpp", "**/*TestSupport.cpp", "**/*Test.mm", "**/*TestSupport.mm")
                        this.headersDirs.setFrom(it.headersDirs)
                        this.headersDirs.from(googleTestExtension.headersDirs)
                        this.group = VERIFICATION_BUILD_TASK_GROUP
                        this.description = "Compiles '${it.name}' tests to bitcode for $target${sanitizer.description}"

                        dependsOn(":kotlin-native:dependencies:update")
                        dependsOn("downloadGoogleTest")

                        addToCompdb(this, target)
                    }
            if (task.inputFiles.count() == 0) null
            else task
        }
        val testFrameworkTasks = testsGroup.testSupportModules.get().map {
            val name = fullTaskName(it, target.name, sanitizer)
            project.tasks.getByName(name) as CompileToBitcode
        }

        val testSupportTask = testsGroup.testLauncherModule.get().let {
            val name = fullTaskName(it, target.name, sanitizer)
            project.tasks.getByName(name) as CompileToBitcode
        }

        val compileTask = project.tasks.register<CompileToExecutable>("${testName}Compile") {
            description = "Compile tests group '$testTaskName' for $target${sanitizer.description}"
            group = VERIFICATION_BUILD_TASK_GROUP
            this.target.set(target)
            this.sanitizer.set(sanitizer)
            this.outputFile.set(project.layout.buildDirectory.file("bin/test/${target}/$testName${target.executableExtension}"))
            this.llvmLinkFirstStageOutputFile.set(project.layout.buildDirectory.file("bitcode/test/$target/$testName-firstStage.bc"))
            this.llvmLinkOutputFile.set(project.layout.buildDirectory.file("bitcode/test/$target/$testName.bc"))
            this.compilerOutputFile.set(project.layout.buildDirectory.file("obj/$target/$testName.o"))
            this.mimallocEnabled.set(testsGroup.testedModules.get().any { it.contains("mimalloc") })
            this.mainFile.set(testSupportTask.outputFile)
            val tasksToLink = (compileToBitcodeTasks + testedTasks + testFrameworkTasks)
            this.inputFiles.setFrom(tasksToLink.map { it.outputFile })

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

            usesService(runGTestSemaphore)
        }

        allTestsTasks[target.name]!!.configure {
            dependsOn(runTask)
        }
    }

    fun testsGroup(
            testTaskName: String,
            action: Action<in TestsGroup>,
    ) {
        platformManager.enabled.forEach { target ->
            val sanitizers: List<SanitizerKind?> = target.supportedSanitizers() + listOf(null)
            sanitizers.forEach { sanitizer ->
                val instance = project.objects.newInstance(TestsGroup::class.java, target, sanitizer.asMaybe).apply {
                    testSupportModules.convention(listOf("googletest", "googlemock"))
                    testLauncherModule.convention("test_support")
                    action.execute(this)
                }
                createTestTask(testTaskName, instance)
            }
        }
    }

    companion object {

        const val BUILD_TASK_GROUP = LifecycleBasePlugin.BUILD_GROUP
        const val VERIFICATION_TASK_GROUP = LifecycleBasePlugin.VERIFICATION_GROUP
        const val VERIFICATION_BUILD_TASK_GROUP = "verification build"

        @OptIn(ExperimentalStdlibApi::class)
        private val String.capitalized: String
            get() = replaceFirstChar { it.uppercase() }

        private fun String.snakeCaseToUpperCamelCase() = split('_').joinToString(separator = "") { it.capitalized }

        private fun fullTaskName(name: String, targetName: String, sanitizer: SanitizerKind?) = "${targetName}${name.snakeCaseToUpperCamelCase()}${sanitizer.taskSuffix}"

        private val SanitizerKind?.taskSuffix
            get() = when (this) {
                null -> ""
                SanitizerKind.ADDRESS -> "_ASAN"
                SanitizerKind.THREAD -> "_TSAN"
            }

        private val SanitizerKind?.dirSuffix
            get() = when (this) {
                null -> ""
                SanitizerKind.ADDRESS -> "-asan"
                SanitizerKind.THREAD -> "-tsan"
            }

        private val SanitizerKind?.description
            get() = when (this) {
                null -> ""
                SanitizerKind.ADDRESS -> " with ASAN"
                SanitizerKind.THREAD -> " with TSAN"
            }

        private val KonanTarget.executableExtension
            get() = when (this) {
                is KonanTarget.MINGW_X64 -> ".exe"
                is KonanTarget.MINGW_X86 -> ".exe"
                else -> ""
            }
    }
}
