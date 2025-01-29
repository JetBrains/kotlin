/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.js

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.gradle.api.BuildCancelledException
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.js.ProcessManager.Companion.processManager
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.fail
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.concurrent.thread
import kotlin.coroutines.CoroutineContext
import kotlin.io.path.*
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

@MppGradlePluginTests
class JsContinuousBuildIT : KGPBaseTest() {

    @GradleTest
    @TestMetadata("js-run-continuous")
//    @Timeout(value = 1, unit = java.util.concurrent.TimeUnit.MINUTES)
    fun testJsRunContinuousBuild2(
        gradleVersion: GradleVersion,
    ) {
//        var i = 0
//        while (i++ < 1_000_000) {
        project("js-run-continuous", gradleVersion) {
            val compiledJs = projectPath.resolve("build/compileSync/js/main/developmentExecutable/kotlin/js-run-continuous.js")

            buildScriptInjection {
                thread(name = "testJsRunContinuousBuild2 kill switch", isDaemon = true) {
                    // add a kill-switch to force-kill the Gradle build on test completion,
                    // because `--continuous` mode disables `--no-daemon`
                    val timeoutMark = TimeSource.Monotonic.markNow() + 5.minutes
                    while (timeoutMark.hasPassedNow() || !Path("end-test").exists()) {
                        Thread.sleep(1000)
                    }
                    throw BuildCancelledException("testJsRunContinuousBuild2 timeout")
                }
            }

            val daemonRelease = PipedOutputStream()
            val daemonStdin = PipedInputStream(daemonRelease)

            val checker = thread(name = "testJsRunContinuousBuild2 checker", isDaemon = true) {
                // wait for the first compilation to succeed
                while (!compiledJs.exists()) {
                    Thread.sleep(1000)
                }

                // modify a file to trigger a re-build
                projectPath.resolve("src/jsMain/kotlin/main.kt")
                    .replaceText("//println", "println")

                // wait for the second compilation to succeed
                while ("Hello again!!!" !in compiledJs.readText()) {
                    Thread.sleep(1000)
                }
                daemonRelease.close()
            }

            build(
                "jsBrowserDevelopmentRun",
                "--continuous",
                buildOptions = defaultBuildOptions.copy(verboseVfs = true),
                inputStream = daemonStdin,
            ) {
                // trigger the Gradle build kill-switch
                projectPath.resolve("end-test").createFile()

                checker.join()

                assertFileContains(
                    compiledJs,
                    /* language=text */ """
                    |  function main() {
                    |    println('Hello, world!');
                    |    println('Hello again!!!');
                    |  }
                    """.trimMargin()
                )

                fun String.getLinesStartingWith(vararg prefixes: String): String {
                    return lines()
                        .filter { line ->
                            prefixes.any { prefix -> line.startsWith(prefix) }
                        }
//                        it.startsWith("[ExecHandle Resolving NPM dependencies using yarn]") }
                        .joinToString("\n")
                }

                // verify yarn dependency resolution starts and stops successfully
                assertEquals(
                    // language=text
                    """
                    |[ExecHandle Resolving NPM dependencies using yarn] Changing state to: Starting
                    |[ExecHandle Resolving NPM dependencies using yarn] Started. Waiting until streams are handled...
                    |[ExecHandle Resolving NPM dependencies using yarn] Changing state to: Initial
                    |[ExecHandle Resolving NPM dependencies using yarn] Waiting until process started
                    |[ExecHandle Resolving NPM dependencies using yarn] Successfully started process
                    |[ExecHandle Resolving NPM dependencies using yarn] Changing state to: Started
                    |[ExecHandle Resolving NPM dependencies using yarn] finished with exit value 0 (state: Succeeded)
                    """.trimMargin(),
                    output.getLinesStartingWith("[ExecHandle Resolving NPM dependencies using yarn]"),
                )

                // verify webpack starts and stops successfully
                assertEquals(
                    // language=text
                    """
                    |[ExecHandle webpack webpack/bin/webpack.js jsmain] Changing state to: Starting
                    |[ExecHandle webpack webpack/bin/webpack.js jsmain] Started. Waiting until streams are handled...
                    |[ExecHandle webpack webpack/bin/webpack.js jsmain] Changing state to: Initial
                    |[ExecHandle webpack webpack/bin/webpack.js jsmain] Waiting until process started
                    |[ExecHandle webpack webpack/bin/webpack.js jsmain] Successfully started process
                    |[:jsBrowserDevelopmentRun] webpack-dev-server started webpack webpack/bin/webpack.js jsmain
                    |[ExecHandle webpack webpack/bin/webpack.js jsmain] Abort requested. Destroying process.
                    |[ExecHandle webpack webpack/bin/webpack.js jsmain] Changing state to: Started
                    |[ExecHandle webpack webpack/bin/webpack.js jsmain] finished with exit value ? (state: Aborted)
                    |[:jsBrowserDevelopmentRun] webpack-dev-server stopped webpack webpack/bin/webpack.js jsmain
                    """.trimMargin(),
                    output.getLinesStartingWith(
                        "[ExecHandle webpack webpack/bin/webpack.js jsmain]",
                        "[:jsBrowserDevelopmentRun]"
                    )
                        .replace(Regex("finished with exit value -?\\d+ "), "finished with exit value ? "),
//                    output.lines().filter { it.startsWith("[ExecHandle webpack webpack/bin/webpack.js jsmain]") }.joinToString("\n"),
                )

                // verify the DeploymentHandle starts and stops successfully
//                assertEquals(
//                    // language=text
//                    """
//                    |[:jsBrowserDevelopmentRun] webpack-dev-server started webpack webpack/bin/webpack.js jsmain
//                    |[:jsBrowserDevelopmentRun] webpack-dev-server stopped webpack webpack/bin/webpack.js jsmain
//                    """.trimMargin(),
//                    output.lines().filter { it.startsWith("[:jsBrowserDevelopmentRun] webpack-dev-server") }.joinToString("\n"),
//                )
            }
        }
//        }
    }


    @GradleTest
    @TestMetadata("js-run-continuous")
    @Timeout(value = 1, unit = java.util.concurrent.TimeUnit.MINUTES)
    @Disabled
    fun testJsRunContinuousBuild(
        gradleVersion: GradleVersion,
    ) {
        project("js-run-continuous", gradleVersion) {
            runBlocking {
                build("wrapper") {
                    assertTasksExecuted(":wrapper")
                }

                println(projectPath.listDirectoryEntries())

                // TODO fetch & run gradlew on Windows
                val gradlew = projectPath.resolve("gradlew")

                println("creating process manager...")
                val process = processManager(
                    workDir = projectPath,
                    commands = buildList {
                        add(gradlew.invariantSeparatorsPathString)
                        add("jsBrowserDevelopmentRun")
                        add("--continuous")
//                        add("--no-daemon") // --no-daemon doesn't work with --continuous
                        addAll(buildOptions.toArguments(gradleVersion))
                    }
                )
                println("created process manager!")

                process.logLines
                    .onEach {
                        if (it == "Exiting continuous build as Gradle does not watch any file system locations.") {
                            fail("Gradle exited unexpectedly! $it")
                        }
                    }
                    .launchIn(this + Dispatchers.IO)

                println("starting process...")
                process.start()
                println("started!")

                // wait for compilation success...
                println("waiting for success1....")
                val success1 =
                    withTimeout(20.seconds) {
                        process.logLines.first { it.contains("BUILD SUCCESSFUL") }
                    }
                println("success1: found 'BUILD SUCCESSFUL' in '$success1'")
                delay(1.seconds)

                println("waiting for Gradle to wait for changes....")
                withTimeout(30.seconds) {
                    process.logLines.first { it.contains("Waiting for changes to input files...") }
                }
                println("Gradle is awaiting changes!")
                delay(1.seconds)

                val compiledJs = projectPath.resolve("build/compileSync/js/main/developmentExecutable/kotlin/js-run-continuous.js")
                println(compiledJs.readText().prependIndent())
                assertFileContains(
                    compiledJs,
                    /* language=text */
                    """
                    |  function main() {
                    |    println('Hello, world!');
                    |  }
                    """.trimMargin()
                )
                assertFileDoesNotContain(compiledJs, "Hello again!!!")

                // verify webpack process launches
                // ...

                // verify compilation succeeds
                // ...

                // make a change in kotlin sources
                println("Modifying a file...")
                projectPath.resolve("src/jsMain/kotlin/main.kt")
                    .replaceText("//println", "println")

                delay(1.seconds)
                println("Awaiting changeDetected1...")
                val changeDetected1 = withTimeout(5.seconds) { process.logLines.first { "Change detected, executing build" in it } }
                println("changeDetected1: $changeDetected1")

                // verify compilation re-runs successfully
                delay(1.seconds)
                println("waiting for success2....")
                val success2 =
                    withTimeout(10.seconds) {
                        process.logLines
                            .dropWhile { "Change detected, executing build" !in it }
                            .first { it.contains("BUILD SUCCESSFUL") }
                    }
                println("success2: $success2")


//                val compiledJs = projectPath.resolve("build/compileSync/js/main/developmentExecutable/kotlin/js-run-continuous.js")
                println(compiledJs.readText().prependIndent())
                assertFileContains(
                    compiledJs,
                    /* language=text */ """
                    |  function main() {
                    |    println('Hello, world!');
                    |    println('Hello again!!!');
                    |  }
                    """.trimMargin()
                )
                println("Finished assertions...")
                delay(1.seconds)

                // disconnect using ctrl+d
                // ...

                // verify webpack process terminates
                // ...


//            build("assemble") {
//                assertTasksExecuted(":jsBrowserProductionWebpack")
//            }

                println("ℹ️Test finished, closing process...")

                process.close()
            }
        }
    }
}

private class ProcessManager private constructor(
    private val workDir: Path,
    private val commands: List<String>,
    private val coroutineContext: CoroutineContext,
    private val redirectErrorStream: Boolean = true,
) : AutoCloseable {

    //    private val coroutineJob = SupervisorJob(coroutineContext.job)
    private val coroutineJob = Job(coroutineContext.job)
    private val coroutineScope = CoroutineScope(coroutineContext) +
            coroutineJob +
            Dispatchers.IO +
            CoroutineName("ProcessManager $commands") +
            CoroutineExceptionHandler { _, it -> close(); throw it }

    private val outputLog = Files.createTempFile("stdout", ".log")

    init {
        println("$outputLog: ${outputLog.toUri()}")
    }

    private val lock: Mutex = Mutex(locked = false)

    private var process: Process? = null

    private val _logLines = MutableSharedFlow<String>(replay = Int.MAX_VALUE, extraBufferCapacity = 1024)
    val logLines: SharedFlow<String> get() = _logLines.asSharedFlow()

    suspend fun start(): Unit = lock.withLock {
        if (process != null) {
            error("Process already started")
        }

        coroutineScope.launch {

            val process = ProcessBuilder(commands)
                .apply {
                    directory(workDir.toFile())
                    redirectErrorStream(redirectErrorStream)
                }
                .start()


            // handle exceptions
            coroutineScope.launch {
                //println("launching exception handler")
                process.onExit().thenAccept {
                    println("process.onExit().thenAccept ${it.exitValue()}...")
                    if (it.exitValue() != 0) {
//                    coroutineJob.cancel("Process exited with non-zero exit code: ${it.exitValue()}")
                        coroutineJob.completeExceptionally(IllegalStateException("Process $commands exited with non-zero exit code: ${it.exitValue()}"))
                    } else {
                        coroutineScope.cancel()
                        coroutineJob.complete()
                    }
                }.exceptionally {
                    println("process.onExit().exceptionally ...")
//                coroutineJob.cancel("Failed to get process exit code", it)
                    coroutineJob.completeExceptionally(IllegalStateException("Process $commands exited with exception", it))
                    throw it
                }.toCompletableFuture().await()
                println("finished exception handler")
            }


            // forward logs
            coroutineScope.launch {
                process.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        println("  ${ansiWhite}${line}${ansiReset}")
                        _logLines.emit(line)
                    }
                }
            }

            // forward all logs to file
            logLines
                .onEach { l -> outputLog.appendText("$l\n") }
                .launchIn(coroutineScope)


            // stop the process when the outer scope completes
            coroutineScope.coroutineContext.job.invokeOnCompletion {
                close()
            }

            this@ProcessManager.process = process
        }
    }

    suspend fun result(
        timeout: Duration = 60.seconds,
    ): ExecResult = lock.withLock {
        val process = requireNotNull(process) { "Cannot get process result because process is not started" }

        if (process.isAlive) {
            process.waitFor(timeout.inWholeMilliseconds, MILLISECONDS)
        }

        coroutineJob.cancel()

        return ExecResult(
            process.exitValue(),
            outputLog,
        )
    }

//            .thenAccept { result ->
//                continuation.resume(
//                    ExecResult(
//                        exitCode = result.exitValue(),
//                        outputLog = outputLog,
//                        command = commands.toString(),
//                    )
//                )
//            }.exceptionally { exception ->
//                continuation.resumeWithException(exception)
//                null
//            }

    override fun close() {
        println("Closing process...")
        val process = process ?: run {
            println("Process not started, or already closed")
            return
        }
        coroutineScope.launch {
            process.outputStream.write("\u0004".toByteArray()) // Ctrl+D is '4'
            println("disconnected")
            process.destroy()
            println("cancelling $coroutineJob children...")
//        coroutineJob.cancelChildren()
//        coroutineJob.complete()
            if (!process.waitFor(5, SECONDS)) {
                println("Force closing process")
                process.destroyForcibly()
            }
            this@ProcessManager.process = null
            println("Closed process!")
        }
    }

    companion object {
        suspend fun processManager(
            workDir: Path,
            commands: List<String>,
        ): ProcessManager {
            return ProcessManager(
                workDir = workDir,
                commands = commands,
                coroutineContext = currentCoroutineContext(),
            )
        }

        private val ansiWhite = "\u001B[37m"
        private val ansiReset = "\u001B[0m"
    }
}

//private data class ExecuteProcessHandle(
//    val output: Flow<String>,
//    val result: Deferred<ExecResult>,
//)

private data class ExecResult(
    val exitCode: Int,
    val outputLog: Path,
//    val command: String,
)
