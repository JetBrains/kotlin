/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.process.CommandLineArgumentProvider
import org.gradle.work.DisableCachingByDefault
import java.io.File

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
