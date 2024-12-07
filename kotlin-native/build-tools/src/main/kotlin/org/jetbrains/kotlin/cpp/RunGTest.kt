/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(kotlin.time.ExperimentalTime::class)
package org.jetbrains.kotlin.cpp

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.konan.target.*
import java.time.Duration
import javax.inject.Inject
import kotlin.time.toKotlinDuration

private abstract class RunGTestJob : WorkAction<RunGTestJob.Parameters> {
    interface Parameters : WorkParameters {
        val testName: Property<String>
        val executable: RegularFileProperty
        val reportFile: RegularFileProperty
        val reportFileUnprocessed: RegularFileProperty
        val filter: Property<String>
        val tsanSuppressionsFile: RegularFileProperty
        val executorsClasspath: ConfigurableFileCollection
        val distPath: Property<String>
        val dataDirPath: Property<String>
        val targetName: Property<String>
        val executionTimeout: Property<Duration>
    }

    @get:Inject
    protected abstract val execOperations: ExecOperations

    override fun execute() {
        // TODO: Try to make it like other gradle test tasks: report progress in a way gradle understands instead of dumping stdout of gtest.

        with(parameters) {
            reportFileUnprocessed.asFile.get().parentFile.mkdirs()

            execOperations.javaexec {
                classpath(executorsClasspath)
                mainClass.set("org.jetbrains.kotlin.native.executors.cli.ExecutorsCLI")
                tsanSuppressionsFile.orNull?.also {
                    environment("TSAN_OPTIONS", "suppressions=${it.asFile.absolutePath}")
                }
                args("--dist=${distPath.get()}")
                dataDirPath.orNull?.also {
                    args("--data-dir=${dataDirPath.get()}")
                }
                args("--target=${targetName.get()}")
                args("--timeout=${executionTimeout.get().toKotlinDuration()}")
                args("--")
                args(this@with.executable.asFile.get().absolutePath)
                args("--gtest_output=xml:${reportFileUnprocessed.asFile.get().absolutePath}")
                filter.orNull?.also {
                    args("--gtest_filter=${it}")
                }
            }.assertNormalExitValue()

            reportFile.asFile.get().parentFile.mkdirs()

            // TODO: Better to use proper XML parsing.
            var contents = reportFileUnprocessed.asFile.get().readText()
            contents = contents.replace("<testsuite name=\"", "<testsuite name=\"${testName.get()}.")
            contents = contents.replace("classname=\"", "classname=\"${testName.get()}.")
            reportFile.asFile.get().writeText(contents)
        }
    }
}

/**
 * Run [googletest](https://google.github.io/googletest) test binary from [executable].
 *
 * Test reports are placed in [reportFileUnprocessed] and in [reportFile] (decorates each test with [testName]).
 *
 * @see CompileToBitcodePlugin
 */
@UntrackedTask(because = "Test executables must always run when asked to")
abstract class RunGTest : DefaultTask() {
    /**
     * Decorating test names in the report.
     *
     * Useful when CI merges different test results of the same test but for different test targets.
     */
    @get:Input
    abstract val testName: Property<String>

    /**
     * Test executable
     */
    @get:InputFile
    abstract val executable: RegularFileProperty

    /**
     * Test report with each test name decorated with [testName].
     */
    @get:OutputFile
    abstract val reportFile: RegularFileProperty

    /**
     * Undecorated test report.
     */
    @get:OutputFile
    abstract val reportFileUnprocessed: RegularFileProperty

    /**
     * Run a subset of tests.
     *
     * Follows [googletest](https://google.github.io/googletest/advanced.html#running-a-subset-of-the-tests) syntax:
     * a ':'-separated list of glob patterns.
     *
     * Examples:
     * * `SomeTest*` - run every test starting with `SomeTest`.
     * * `SomeTest*:*stress*` - run every test starting with `SomeTest` and also every test containing `stress`.
     * * `SomeTest*:-SomeTest.flakyTest` - Run every test starting with `SomeTest` except `SomeTest.flakyTest`.
     */
    @get:Input
    @get:Optional
    abstract val filter: Property<String>

    /**
     * Suppression rules for TSAN.
     */
    @get:InputFile
    @get:Optional
    abstract val tsanSuppressionsFile: RegularFileProperty

    @get:Inject
    protected abstract val workerExecutor: WorkerExecutor

    @get:Input
    abstract val target: Property<KonanTarget>

    /**
     * Timeout for the test run.
     */
    @get:Input
    abstract val executionTimeout: Property<Duration>

    @get:InputFiles
    abstract val executorsClasspath: ConfigurableFileCollection

    @get:Input
    abstract val distPath: Property<String>

    @get:Input
    @get:Optional
    abstract val dataDirPath: Property<String>

    @TaskAction
    fun run() {
        val workQueue = workerExecutor.noIsolation()

        workQueue.submit(RunGTestJob::class.java) {
            testName.set(this@RunGTest.testName)
            executable.set(this@RunGTest.executable)
            reportFile.set(this@RunGTest.reportFile)
            reportFileUnprocessed.set(this@RunGTest.reportFileUnprocessed)
            filter.set(this@RunGTest.filter)
            tsanSuppressionsFile.set(this@RunGTest.tsanSuppressionsFile)
            executorsClasspath.from(this@RunGTest.executorsClasspath)
            distPath.set(this@RunGTest.distPath)
            dataDirPath.set(this@RunGTest.dataDirPath)
            targetName.set(this@RunGTest.target.get().name)
            executionTimeout.set(this@RunGTest.executionTimeout)
        }
    }
}
