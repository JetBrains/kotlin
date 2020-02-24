/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin

import groovy.lang.Closure
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.tasks.*
import java.io.File


open class VersionGenerator: DefaultTask() {
    @OutputDirectory
    val versionSourceDirectory = project.file("build/generated")
    @OutputFile
    val versionFile:File = project.file("${versionSourceDirectory.path}/org/jetbrains/kotlin/konan/CompilerVersionGenerated.kt")

    val konanVersion: String
        @Input get() = project.properties["konanVersion"].toString()

    val buildNumber: String?
        // TeamCity passes all configuration parameters into a build script as project properties.
        // Thus we can use them here instead of environment variables.
        @Optional @Input get() = project.findProperty("build.number")?.toString()

    val meta: String
        @Input get() = project.properties["konanMetaVersion"]?.let {
            "MetaVersion.${it.toString().toUpperCase()}"
        } ?: "MetaVersion.DEV"


    private val versionPattern = "(\\d+)\\.(\\d+)(?:\\.(\\d+))?(?:-M(\\p{Digit}))?(?:-(\\p{Alpha}\\p{Alnum}*))?(?:-(\\d+))?".toRegex()

    override fun configure(closure: Closure<*>): Task {
        val result = super.configure(closure)
        doFirst {
            val content = buildString {
                operator fun String.unaryPlus() = this@buildString.append(this)
                val (majorStr, minorStr, maintenanceStr, milestoneStr) =
                        versionPattern.matchEntire(konanVersion)?.destructured
                                ?: throw IllegalArgumentException("Cannot parse Kotlin/Native version: $konanVersion")
                val major = majorStr.toInt()
                val minor = minorStr.toInt()
                val maintenance = maintenanceStr.toIntOrNull() ?: 0
                val milestone = milestoneStr.toIntOrNull() ?: -1
                project.logger.info("BUILD_NUMBER: $buildNumber")
                val build = buildNumber?.let {
                    it.split("-").last().toInt() //7-dev-buildcount
                }?: -1

                + """
                   |package org.jetbrains.kotlin.konan
                   |
                   |internal val currentCompilerVersion: CompilerVersion =
                   |    CompilerVersionImpl($meta, $major, $minor, $maintenance, $milestone, $build)
                   |
                   |val CompilerVersion.Companion.CURRENT: CompilerVersion
                   |    get() = currentCompilerVersion
                """.trimMargin()
            }
            versionFile.printWriter().use {
                it.println(content)
            }
        }
        return result
    }
}
