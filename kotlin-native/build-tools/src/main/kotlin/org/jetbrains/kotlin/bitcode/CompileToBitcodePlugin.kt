/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bitcode

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.BasePlugin
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.ExecClang
import org.jetbrains.kotlin.cpp.CompilationDatabaseExtension
import org.jetbrains.kotlin.cpp.CompilationDatabasePlugin
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.PlatformManager
import org.jetbrains.kotlin.konan.target.SanitizerKind
import org.jetbrains.kotlin.konan.target.supportedSanitizers
import org.jetbrains.kotlin.testing.native.CompileNativeTest
import org.jetbrains.kotlin.testing.native.GoogleTestExtension
import org.jetbrains.kotlin.testing.native.RunNativeTest
import org.jetbrains.kotlin.testing.native.RuntimeTestingPlugin
import java.io.File
import javax.inject.Inject

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

    private val compilationDatabase = project.extensions.getByType<CompilationDatabaseExtension>()
    private val execClang = project.extensions.getByType<ExecClang>()

    private val targetList = with(project) {
        provider { (rootProject.project(":kotlin-native").property("targetList") as? List<*>)?.filterIsInstance<String>() ?: emptyList() } // TODO: Can we make it better?
    }

    private val allMainModulesTasks by lazy {
        val name = project.name.capitalized
        targetList.get().associateBy(keySelector = { it }, valueTransform = {
            project.tasks.register("${it}$name")
        })
    }

    private val allTestsTasks by lazy {
        val name = project.name.capitalized
        targetList.get().associateBy(keySelector = { it }, valueTransform = {
            project.tasks.register("${it}${name}Tests")
        })
    }

    private fun addToCompdb(compileTask: CompileToBitcode, konanTarget: KonanTarget) {
        // No need to generate compdb entry for sanitizers.
        if (compileTask.sanitizer != null) {
            return
        }
        compilationDatabase.target(konanTarget) {
            entry {
                val args = listOf(execClang.resolveExecutable(compileTask.executable)) + compileTask.compilerFlags + execClang.clangArgsForCppRuntime(konanTarget.name)
                directory.set(compileTask.objDir)
                files.setFrom(compileTask.inputFiles)
                arguments.set(args)
                output.set(compileTask.outFile.absolutePath)
            }
        }
    }

    fun module(name: String, srcRoot: File = project.file("src/$name"), outputGroup: String = "main", configurationBlock: CompileToBitcode.() -> Unit = {}) {
        targetList.get().forEach { targetName ->
            val platformManager = project.rootProject.project(":kotlin-native").findProperty("platformManager") as PlatformManager
            val target = platformManager.targetByName(targetName)
            val sanitizers: List<SanitizerKind?> = target.supportedSanitizers() + listOf(null)
            val allMainModulesTask = allMainModulesTasks[targetName]!!
            sanitizers.forEach { sanitizer ->
                val taskName = fullTaskName(name, targetName, sanitizer)
                val task = project.tasks.create(taskName, CompileToBitcode::class.java, name, targetName, outputGroup).apply {
                    srcDirs = project.files(srcRoot.resolve("cpp"))
                    headersDirs = srcDirs + project.files(srcRoot.resolve("headers"))

                    this.sanitizer = sanitizer
                    group = BasePlugin.BUILD_GROUP
                    val sanitizerDescription = when (sanitizer) {
                        null -> ""
                        SanitizerKind.ADDRESS -> " with ASAN"
                        SanitizerKind.THREAD -> " with TSAN"
                    }
                    description = "Compiles '$name' to bitcode for $targetName$sanitizerDescription"
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

    private fun createTestTask(
            project: Project,
            testName: String,
            testedTaskNames: List<String>,
            sanitizer: SanitizerKind?,
    ): Task {
        val platformManager = project.project(":kotlin-native").findProperty("platformManager") as PlatformManager
        val googleTestExtension = project.extensions.getByName(RuntimeTestingPlugin.GOOGLE_TEST_EXTENSION_NAME) as GoogleTestExtension
        val testedTasks = testedTaskNames.map {
            project.tasks.getByName(it) as CompileToBitcode
        }
        val target = testedTasks.map {
            it.target
        }.distinct().single()
        val konanTarget = platformManager.targetByName(target)
        val compileToBitcodeTasks = testedTasks.mapNotNull {
            val name = "${it.name}TestBitcode"
            val task = project.tasks.findByName(name) as? CompileToBitcode
                    ?: project.tasks.create(name, CompileToBitcode::class.java, "${it.folderName}Tests", target, "test").apply {
                        srcDirs = it.srcDirs
                        headersDirs = it.headersDirs + googleTestExtension.headersDirs

                        this.sanitizer = sanitizer
                        excludeFiles = emptyList()
                        includeFiles = listOf("**/*Test.cpp", "**/*TestSupport.cpp", "**/*Test.mm", "**/*TestSupport.mm")
                        dependsOn(":kotlin-native:dependencies:update")
                        dependsOn("downloadGoogleTest")
                        compilerArgs.addAll(it.compilerArgs)

                        addToCompdb(this, konanTarget)
                    }
            if (task.inputFiles.count() == 0) null
            else task
        }
        // TODO: Consider using sanitized versions.
        val testFrameworkTasks = listOf(project.tasks.getByName(fullTaskName("googletest", target, null)) as CompileToBitcode, project.tasks.getByName(fullTaskName("googlemock", target, null)) as CompileToBitcode)

        val testSupportTask = project.tasks.getByName(fullTaskName("test_support", target, sanitizer)) as CompileToBitcode

        val mimallocEnabled = testedTaskNames.any { it.contains("mimalloc", ignoreCase = true) }
        val compileTask = project.tasks.create(
                "${testName}Compile",
                CompileNativeTest::class.java,
                testName,
                konanTarget,
                testSupportTask.outFile,
                platformManager,
                mimallocEnabled,
        ).apply {
            val tasksToLink = (compileToBitcodeTasks + testedTasks + testFrameworkTasks)
            this.sanitizer = sanitizer
            this.inputFiles.setFrom(tasksToLink.map { it.outFile })
            dependsOn(testSupportTask)
            dependsOn(tasksToLink)
        }

        val runTask = project.tasks.create(
                testName,
                RunNativeTest::class.java,
                testName,
                compileTask.outputFile,
        ).apply {
            this.sanitizer = sanitizer
            dependsOn(compileTask)
        }

        return runTask
    }

    fun testsGroup(
            testTaskName: String,
            testedTaskNames: List<String>,
    ) {
        val platformManager = project.rootProject.project(":kotlin-native").findProperty("platformManager") as PlatformManager
        targetList.get().forEach { targetName ->
            val target = platformManager.targetByName(targetName)
            val sanitizers: List<SanitizerKind?> = target.supportedSanitizers() + listOf(null)
            val allTestsTask = allTestsTasks[targetName]!!
            sanitizers.forEach { sanitizer ->
                val name = fullTaskName(testTaskName, targetName, sanitizer)
                val testedNames = testedTaskNames.map {
                    fullTaskName(it, targetName, sanitizer)
                }
                val task = createTestTask(project, name, testedNames, sanitizer)
                allTestsTask.configure {
                    dependsOn(task)
                }
            }
        }
    }

    companion object {

        private const val COMPILATION_DATABASE_TASK_NAME = "CompilationDatabase"

        private val String.capitalized: String
            get() = replaceFirstChar { it.uppercase() }

        private fun String.snakeCaseToUpperCamelCase() = split('_').joinToString(separator = "") { it.capitalized }

        private fun fullTaskName(name: String, targetName: String, sanitizer: SanitizerKind?) = "${targetName}${name.snakeCaseToUpperCamelCase()}${sanitizer.suffix}"

        private val SanitizerKind?.suffix
            get() = when (this) {
                null -> ""
                SanitizerKind.ADDRESS -> "_ASAN"
                SanitizerKind.THREAD -> "_TSAN"
            }

    }
}
