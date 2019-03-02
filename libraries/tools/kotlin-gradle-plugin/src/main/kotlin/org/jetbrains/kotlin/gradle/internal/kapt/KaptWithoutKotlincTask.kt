/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.workers.IsolationMode
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.incremental.ChangedFiles
import org.jetbrains.kotlin.gradle.internal.Kapt3KotlinGradleSubplugin.Companion.KAPT_WORKER_DEPENDENCIES_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinAndroidPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.clearLocalState
import org.jetbrains.kotlin.gradle.tasks.findKotlinStdlibClasspath
import org.jetbrains.kotlin.gradle.tasks.findToolsJar
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.io.Serializable
import java.net.URLClassLoader
import javax.inject.Inject

open class KaptWithoutKotlincTask @Inject constructor(private val workerExecutor: WorkerExecutor) : KaptTask() {
    @get:InputFiles
    @get:Classpath
    @Suppress("unused")
    val kaptJars: Collection<File>
        get() = project.configurations.getByName(KAPT_WORKER_DEPENDENCIES_CONFIGURATION_NAME).resolve()

    @get:Input
    var isVerbose: Boolean = false

    @get:Input
    var mapDiagnosticLocations: Boolean = false

    @get:Input
    lateinit var annotationProcessorFqNames: List<String>

    @get:Input
    lateinit var processorOptions: Map<String, String>

    @get:Input
    lateinit var javacOptions: Map<String, String>

    @TaskAction
    fun compile(inputs: IncrementalTaskInputs) {
        logger.info("Running kapt annotation processing using the Gradle Worker API")
        checkAnnotationProcessorClasspath()

        val compileClasspath = classpath.files.toMutableList()
        if (project.plugins.none { it is KotlinAndroidPluginWrapper }) {
            compileClasspath.addAll(0, PathUtil.getJdkClassesRootsFromCurrentJre())
        }

        val kaptFlagsForWorker = mutableSetOf<String>().apply {
            if (isVerbose) add("VERBOSE")
            if (mapDiagnosticLocations) add("MAP_DIAGNOSTIC_LOCATIONS")
            if (includeCompileClasspath) add("INCLUDE_COMPILE_CLASSPATH")
        }

        val optionsForWorker = KaptOptionsForWorker(
            project.projectDir,
            compileClasspath,
            javaSourceRoots.toList(),

            getChangedFiles(inputs),
            getCompiledSources(),
            incAptCache,
            classpathDirtyFqNamesHistoryDir.singleOrNull(),

            destinationDir,
            classesDir,
            stubsDir,

            kaptClasspath.files.toList(),
            annotationProcessorFqNames,

            processorOptions,
            javacOptions,

            kaptFlagsForWorker
        )

        // Skip annotation processing if no annotation processors were provided.
        if (annotationProcessorFqNames.isEmpty() && kaptClasspath.isEmpty())
            return

        val kaptClasspath = kaptJars + findKotlinStdlibClasspath(project)

        workerExecutor.submit(KaptExecution::class.java) { config ->
            val isolationModeStr = project.findProperty("kapt.workers.isolation") as String? ?: "none"
            config.isolationMode = when(isolationModeStr.toLowerCase()) {
                "process" -> IsolationMode.PROCESS
                "none" -> IsolationMode.NONE
                else -> IsolationMode.NONE
            }
            config.params(optionsForWorker, findToolsJar(), kaptClasspath)
            if (project.findProperty("kapt.workers.log.classloading") == "true") {
                // for tests
                config.forkOptions.jvmArgs("-verbose:class")
            }
            logger.info("Kapt worker classpath: ${config.classpath}")
        }
    }
}

private class KaptExecution @Inject constructor(
    val optionsForWorker: KaptOptionsForWorker,
    val toolsJar: File?,
    val kaptClasspath: List<File>
) : Runnable {
    private companion object {
        private const val JAVAC_CONTEXT_CLASS = "com.sun.tools.javac.util.Context"

        private fun kaptClass(classLoader: ClassLoader) = Class.forName("org.jetbrains.kotlin.kapt3.base.Kapt", true, classLoader)
        private var cachedClassLoaderWithToolsJar: ClassLoader? = null
        private var cachedKaptClassLoader: ClassLoader? = null
    }

    override fun run(): Unit = with(optionsForWorker) {
        val kaptClasspathUrls = kaptClasspath.map { it.toURI().toURL() }.toTypedArray()
        val rootClassLoader = findRootClassLoader()

        val classLoaderWithToolsJar = cachedClassLoaderWithToolsJar ?: if (toolsJar != null && !javacIsAlreadyHere()) {
            URLClassLoader(arrayOf(toolsJar.toURI().toURL()), rootClassLoader)
        } else {
            rootClassLoader
        }
        cachedClassLoaderWithToolsJar = classLoaderWithToolsJar

        val kaptClassLoader = cachedKaptClassLoader ?: URLClassLoader(kaptClasspathUrls, classLoaderWithToolsJar)
        cachedKaptClassLoader = kaptClassLoader

        val kaptMethod = kaptClass(kaptClassLoader).declaredMethods.single { it.name == "kapt" }
        kaptMethod.invoke(null, createKaptOptions(kaptClassLoader))
    }

    private fun javacIsAlreadyHere(): Boolean {
        return try {
            Class.forName(JAVAC_CONTEXT_CLASS, false, KaptExecution::class.java.classLoader) != null
        } catch (e: Throwable) {
            false
        }
    }

    private fun createKaptOptions(classLoader: ClassLoader) = with (optionsForWorker) {
        val flags = kaptClass(classLoader).declaredMethods.single { it.name == "kaptFlags" }.invoke(null, flags)

        val mode = Class.forName("org.jetbrains.kotlin.base.kapt3.AptMode", true, classLoader)
            .enumConstants.single { (it as Enum<*>).name == "APT_ONLY" }

        val detectMemoryLeaksMode = Class.forName("org.jetbrains.kotlin.base.kapt3.DetectMemoryLeaksMode", true, classLoader)
            .enumConstants.single { (it as Enum<*>).name == "NONE" }

        Class.forName("org.jetbrains.kotlin.base.kapt3.KaptOptions", true, classLoader).constructors.single().newInstance(
            projectBaseDir,
            compileClasspath,
            javaSourceRoots,

            changedFiles,
            compiledSources,
            incAptCache,
            classpathFqNamesHistory,

            sourcesOutputDir,
            classesOutputDir,
            stubsOutputDir,
            stubsOutputDir, // sic!

            processingClasspath,
            processors,

            processingOptions,
            javacOptions,

            flags,
            mode,
            detectMemoryLeaksMode
        )
    }

    private fun findRootClassLoader(): ClassLoader {
        tailrec fun parentOrSelf(classLoader: ClassLoader): ClassLoader {
            val parent = classLoader.parent ?: return classLoader
            return parentOrSelf(parent)
        }
        return parentOrSelf(KaptExecution::class.java.classLoader)
    }
}

private data class KaptOptionsForWorker(
    val projectBaseDir: File,
    val compileClasspath: List<File>,
    val javaSourceRoots: List<File>,

    val changedFiles: List<File>,
    val compiledSources: List<File>,
    val incAptCache: File?,
    val classpathFqNamesHistory: File?,

    val sourcesOutputDir: File,
    val classesOutputDir: File,
    val stubsOutputDir: File,

    val processingClasspath: List<File>,
    val processors: List<String>,

    val processingOptions: Map<String, String>,
    val javacOptions: Map<String, String>,

    val flags: Set<String>
) : Serializable