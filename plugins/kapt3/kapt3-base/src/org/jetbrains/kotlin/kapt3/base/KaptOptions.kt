/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.base.kapt3

import org.jetbrains.kotlin.kapt3.base.incremental.SourcesToReprocess
import java.io.File
import java.nio.file.Files

private const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"

class KaptOptions(
    val projectBaseDir: File?,
    val compileClasspath: List<File>,
    val javaSourceRoots: List<File>,

    val changedFiles: List<File>,
    val compiledSources: List<File>,
    val incrementalCache: File?,
    val classpathChanges: List<String>,

    val sourcesOutputDir: File,
    val classesOutputDir: File,
    val stubsOutputDir: File,
    val incrementalDataOutputDir: File?,

    val processingClasspath: List<File>,
    val processors: List<String>,

    val processingOptions: Map<String, String>,
    val javacOptions: Map<String, String>,

    val flags: KaptFlags,

    val mode: AptMode,
    val detectMemoryLeaks: DetectMemoryLeaksMode
) : KaptFlags {
    override fun get(flag: KaptFlag) = flags[flag]

    class Builder {
        var projectBaseDir: File? = null
        val compileClasspath: MutableList<File> = mutableListOf()
        val javaSourceRoots: MutableList<File> = mutableListOf()

        val changedFiles: MutableList<File> = mutableListOf()
        val compiledSources: MutableList<File> = mutableListOf()
        var incrementalCache: File? = null
        val classpathChanges: MutableList<String> = mutableListOf()

        var sourcesOutputDir: File? = null
        var classesOutputDir: File? = null
        var stubsOutputDir: File? = null
        var incrementalDataOutputDir: File? = null

        val processingClasspath: MutableList<File> = mutableListOf()
        val processors: MutableList<String> = mutableListOf()

        val processingOptions: MutableMap<String, String> = mutableMapOf()
        val javacOptions: MutableMap<String, String> = mutableMapOf()

        val flags: MutableSet<KaptFlag> = mutableSetOf(
            KaptFlag.USE_LIGHT_ANALYSIS,
            KaptFlag.INCLUDE_COMPILE_CLASSPATH
        )

        var mode: AptMode = AptMode.WITH_COMPILATION
        var detectMemoryLeaks: DetectMemoryLeaksMode = DetectMemoryLeaksMode.DEFAULT

        fun build(): KaptOptions {
            val sourcesOutputDir = this.sourcesOutputDir ?: error("'sourcesOutputDir' must be set")
            val classesOutputDir = this.classesOutputDir ?: error("'classesOutputDir' must be set")
            val stubsOutputDir = this.stubsOutputDir ?: error("'stubsOutputDir' must be set")

            return KaptOptions(
                projectBaseDir, compileClasspath, javaSourceRoots,
                changedFiles, compiledSources, incrementalCache, classpathChanges,
                sourcesOutputDir, classesOutputDir, stubsOutputDir, incrementalDataOutputDir,
                processingClasspath, processors, processingOptions, javacOptions, KaptFlags.fromSet(flags),
                mode, detectMemoryLeaks
            )
        }
    }

    fun getKotlinGeneratedSourcesDirectory(): File? {
        val value = processingOptions[KAPT_KOTLIN_GENERATED_OPTION_NAME] ?: return null
        return File(value).takeIf { it.exists() }
    }
}

interface KaptFlags {
    operator fun get(flag: KaptFlag): Boolean

    companion object {
        fun fromSet(flags: Set<KaptFlag>) = object : KaptFlags {
            override fun get(flag: KaptFlag) = flag in flags
        }
    }

    object Empty : KaptFlags {
        override fun get(flag: KaptFlag) = false
    }
}

enum class KaptFlag(val description: String) {
    SHOW_PROCESSOR_TIMINGS("Show processor time"),
    VERBOSE("Verbose mode"),
    INFO_AS_WARNINGS("Info as warnings"),
    USE_LIGHT_ANALYSIS("Use light analysis"),
    CORRECT_ERROR_TYPES("Correct error types"),
    MAP_DIAGNOSTIC_LOCATIONS("Map diagnostic locations"),
    STRICT("Strict mode"),
    INCLUDE_COMPILE_CLASSPATH("Detect annotation processors in compile classpath"),
    INCREMENTAL_APT("Incremental annotation processing (apt mode)"),
    ;
}

interface KaptSelector {
    val stringValue: String
}

enum class DetectMemoryLeaksMode(override val stringValue: String) : KaptSelector {
    DEFAULT("default"), PARANOID("paranoid"), NONE("none")
}

enum class AptMode(override val stringValue: String) : KaptSelector {
    WITH_COMPILATION("compile"),
    STUBS_AND_APT("stubsAndApt"),
    STUBS_ONLY("stubs"),
    APT_ONLY("apt");

    val runAnnotationProcessing
        get() = this != STUBS_ONLY

    val generateStubs
        get() = this != APT_ONLY
}

fun KaptOptions.collectJavaSourceFiles(sourcesToReprocess: SourcesToReprocess = SourcesToReprocess.FullRebuild): List<File> {
    fun allSources(): List<File> {
        return (javaSourceRoots + stubsOutputDir)
            .sortedBy { Files.isSymbolicLink(it.toPath()) } // Get non-symbolic paths first
            .flatMap { root -> root.walk().filter { it.isFile && it.extension == "java" }.toList() }
            .sortedBy { Files.isSymbolicLink(it.toPath()) } // This time is for .java files
            .distinctBy { it.canonicalPath }
    }

    return when (sourcesToReprocess) {
        is SourcesToReprocess.FullRebuild -> allSources()
        is SourcesToReprocess.Incremental -> {
            val toReprocess = sourcesToReprocess.toReprocess.filter { it.exists() }
            if (toReprocess.isNotEmpty()) {
                // Make sure to add error/NonExistentClass.java when there are sources to re-process, as
                // this class is never reported as changed. See https://youtrack.jetbrains.com/issue/KT-34194 for details.
                val nonExistentClass = stubsOutputDir.resolve("error/NonExistentClass.java")
                if (nonExistentClass.exists()) {
                    toReprocess + nonExistentClass
                } else {
                    toReprocess
                }
            } else {
                emptyList()
            }
        }
    }
}

fun KaptOptions.logString(additionalInfo: String = "") = buildString {
    val additionalInfoRendered = if (additionalInfo.isEmpty()) "" else " ($additionalInfo)"
    appendln("Kapt3 is enabled$additionalInfoRendered.")

    appendln("Annotation processing mode: ${mode.stringValue}")
    appendln("Memory leak detection mode: ${detectMemoryLeaks.stringValue}")
    KaptFlag.values().forEach { appendln(it.description + ": " + this@logString[it]) }

    appendln("Project base dir: $projectBaseDir")
    appendln("Compile classpath: " + compileClasspath.joinToString())
    appendln("Java source roots: " + javaSourceRoots.joinToString())

    appendln("Sources output directory: $sourcesOutputDir")
    appendln("Class files output directory: $classesOutputDir")
    appendln("Stubs output directory: $stubsOutputDir")
    appendln("Incremental data output directory: $incrementalDataOutputDir")

    appendln("Annotation processing classpath: " + processingClasspath.joinToString())
    appendln("Annotation processors: " + processors.joinToString())

    appendln("AP options: $processingOptions")
    appendln("Javac options: $javacOptions")

    appendln("[incremental apt] Changed files: $changedFiles")
    appendln("[incremental apt] Compiled sources directories: ${compiledSources.joinToString()}")
    appendln("[incremental apt] Cache directory for incremental compilation: $incrementalCache")
    appendln("[incremental apt] Changed classpath names: ${classpathChanges.joinToString()}")
}