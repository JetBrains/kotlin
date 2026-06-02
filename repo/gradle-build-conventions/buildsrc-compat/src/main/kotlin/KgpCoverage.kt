/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.TestSuiteName
import org.gradle.api.attributes.VerificationType
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.named
import org.gradle.process.CommandLineArgumentProvider
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Registers a consumable outgoing configuration that exposes a JaCoCo `.exec` file to
 * `jacoco-report-aggregation` consumers
 */
fun Project.registerKgpTestCoverageDataVariant(
    configurationName: String,
    suiteName: String,
    execFile: Provider<RegularFile>,
    testTask: TaskProvider<*>,
) {
    configurations.consumable(configurationName) {
        attributes {
            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.VERIFICATION))
            attribute(VerificationType.VERIFICATION_TYPE_ATTRIBUTE, objects.named(VerificationType.JACOCO_RESULTS))
            attribute(TestSuiteName.TEST_SUITE_NAME_ATTRIBUTE, objects.named(suiteName))
        }
        outgoing.artifact(execFile) {
            type = ArtifactTypeDefinition.BINARY_DATA_TYPE
            builtBy(testTask)
        }
    }
}

/**
 * Offline-instruments KGP JARs in Maven Local with JaCoCo probes.
 *
 * This embeds probes into the bytecode before Gradle TestKit applies its own transforms,
 * avoiding conflicts between Gradle's instrumentation in TestKit and JaCoCo's on-the-fly agent.
 */
@DisableCachingByDefault(because = "Modifies external files in Maven Local")
abstract class InstrumentKgpJarsForCoverage : org.gradle.api.tasks.JavaExec() {

    @get:Input
    abstract val kotlinVersion: Property<String>

    @get:Input
    abstract val mavenLocalDir: Property<String>

    @get:Input
    abstract val artifactIds: ListProperty<String>

    init {
        mainClass.set("org.jacoco.cli.internal.Main")

        argumentProviders.add(CommandLineArgumentProvider {
            val mavenLocal = File(mavenLocalDir.get())
            val version = kotlinVersion.get()
            buildList {
                add("instrument")
                for (artifactId in artifactIds.get()) {
                    val jarFile = mavenLocal.resolve("org/jetbrains/kotlin/$artifactId/$version/$artifactId-$version.jar")
                    if (jarFile.exists()) add(jarFile.absolutePath)
                }
                add("--dest")
                add(temporaryDir.absolutePath)
            }
        })

        doLast {
            val mavenLocal = File(mavenLocalDir.get())
            val version = kotlinVersion.get()
            for (artifactId in artifactIds.get()) {
                val jarFile = mavenLocal.resolve("org/jetbrains/kotlin/$artifactId/$version/$artifactId-$version.jar")
                val instrumentedJar = temporaryDir.resolve(jarFile.name)
                if (instrumentedJar.exists()) {
                    instrumentedJar.copyTo(jarFile, overwrite = true)
                    logger.lifecycle("Instrumented $artifactId JAR for JaCoCo offline coverage: ${jarFile.absolutePath}")
                } else {
                    logger.warn("KGP JAR not found for instrumentation: $jarFile")
                }
            }
        }
    }
}

/**
 * Stops the JaCoCo-instrumented Gradle TestKit daemons so their coverage is flushed to disk.
 *
 * Integration-test coverage is collected by an offline JaCoCo agent injected into the TestKit daemon
 * JVMs (see `testDsl.kt#addJacocoAgentIfEnabled`). In `output=file` mode the agent writes the `.exec`
 * only from its JVM shutdown hook (`dumponexit`), and TestKit daemons are long-lived: they outlive the
 * test task and cannot be disabled (`GradleRunner` rejects `--no-daemon`). Without this step the coverage
 * report would read a partial `.exec` containing only whatever daemons happened to have already exited.
 *
 * This task gracefully terminates those daemons ([ProcessHandle.destroy], i.e. SIGTERM) and waits for
 * each to exit, so every shutdown hook finishes its append before the report consumes the file. Stopping
 * them one-by-one also serializes the appends into the shared `.exec`, avoiding interleaved writes.
 */
@DisableCachingByDefault(because = "Terminates external daemon processes; produces no cacheable output")
abstract class FlushTestKitCoverageDaemons : DefaultTask() {

    /** Absolute path of this suite's `.exec` file; used both to identify the daemons and for logging. */
    @get:Internal
    abstract val execFile: RegularFileProperty

    @TaskAction
    fun flush() {
        val execPath = execFile.get().asFile.absolutePath
        val selfPid = ProcessHandle.current().pid()

        // Instrumented daemons carry `-Djacoco-agent.destfile=<execPath>` on their command line.
        fun ProcessHandle.isInstrumentedDaemon(): Boolean {
            val info = info()
            val parts = ArrayList<String>()
            info.command().ifPresent { parts.add(it) }
            info.commandLine().ifPresent { parts.add(it) }
            info.arguments().ifPresent { parts.addAll(it.asList()) }
            return parts.any { execPath in it }
        }

        val daemons = mutableListOf<ProcessHandle>()
        ProcessHandle.allProcesses().forEach { handle ->
            if (handle.pid() != selfPid && handle.isInstrumentedDaemon()) {
                daemons.add(handle)
            }
        }

        if (daemons.isEmpty()) {
            logger.lifecycle("No live JaCoCo-instrumented TestKit daemons found to flush ($execPath).")
            return
        }

        logger.lifecycle("Flushing JaCoCo coverage from ${daemons.size} TestKit daemon(s) into $execPath")
        for (daemon in daemons) {
            daemon.destroy() // SIGTERM -> JVM shutdown hooks -> JaCoCo dumponexit flush
            val exited = runCatching { daemon.onExit().get(DAEMON_EXIT_TIMEOUT_SECONDS, TimeUnit.SECONDS) }.isSuccess
            if (!exited) {
                logger.warn(
                    "TestKit daemon (pid=${daemon.pid()}) did not exit within ${DAEMON_EXIT_TIMEOUT_SECONDS}s; " +
                            "its coverage may be missing. Not force-killing it, as SIGKILL would discard the JaCoCo dump."
                )
            }
        }
        logger.lifecycle("TestKit coverage flush complete.")
    }

    private companion object {
        const val DAEMON_EXIT_TIMEOUT_SECONDS = 60L
    }
}
