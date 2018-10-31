/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.IsolationMode
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.internal.Kapt3KotlinGradleSubplugin.Companion.KAPT_WORKER_DEPENDENCIES_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinAndroidPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.clearOutputDirectories
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
    fun compile() {
        logger.info("Running kapt annotation processing using the Gradle Worker API")

        clearOutputDirectories()

        val compileClasspath = classpath.files.toMutableList()
        if (project.plugins.none { it is KotlinAndroidPluginWrapper }) {
            compileClasspath.addAll(0, PathUtil.getJdkClassesRootsFromCurrentJre())
        }

        val paths = KaptPathsForWorker(
            project.projectDir,
            compileClasspath,
            kaptClasspath.files.toList(),
            javaSourceRoots.toList(),
            destinationDir,
            classesDir,
            stubsDir
        )

        val options = KaptOptionsForWorker(
            isVerbose,
            mapDiagnosticLocations,
            annotationProcessorFqNames,
            processorOptions,
            javacOptions
        )

        val kaptClasspath = kaptJars + findKotlinStdlibClasspath(project)

        workerExecutor.submit(KaptExecution::class.java) { config ->
            config.isolationMode = IsolationMode.PROCESS
            config.params(options, paths, findToolsJar(), kaptClasspath)

            logger.info("Kapt worker classpath: ${config.classpath}")
        }

        workerExecutor.await()
    }
}

private class KaptExecution @Inject constructor(
    val options: KaptOptionsForWorker,
    val paths: KaptPathsForWorker,
    val toolsJar: File?,
    val kaptClasspath: List<File>
) : Runnable {
    private companion object {
        private const val JAVAC_CONTEXT_CLASS = "com.sun.tools.javac.util.Context"
    }

    override fun run(): Unit = with(options) {
        val kaptClasspathUrls = kaptClasspath.map { it.toURI().toURL() }.toTypedArray()

        val rootClassLoader = findRootClassLoader()

        val classLoaderWithToolsJar = if (toolsJar != null && !javacIsAlreadyHere()) {
            toolsJar.let { URLClassLoader(arrayOf(it.toURI().toURL()), rootClassLoader) }
        } else {
            rootClassLoader
        }

        val kaptClassLoader = URLClassLoader(kaptClasspathUrls, classLoaderWithToolsJar)

        val kaptMethod = Class.forName("org.jetbrains.kotlin.kapt3.base.Kapt", true, kaptClassLoader)
            .declaredMethods.single { it.name == "kapt" }

        kaptMethod.invoke(
            null,
            createKaptPaths(kaptClassLoader),
            isVerbose,
            mapDiagnosticLocations,
            annotationProcessorFqNames,
            processorOptions,
            javacOptions
        )
    }

    private fun javacIsAlreadyHere(): Boolean {
        return try {
            Class.forName(JAVAC_CONTEXT_CLASS, false, KaptExecution::class.java.classLoader) != null
        } catch (e: Throwable) {
            false
        }
    }

    private fun createKaptPaths(classLoader: ClassLoader) = with(paths) {
        Class.forName("org.jetbrains.kotlin.kapt3.base.KaptPaths", true, classLoader).constructors.single().newInstance(
            projectBaseDir,
            compileClasspath,
            annotationProcessingClasspath,
            javaSourceRoots,
            sourcesOutputDir,
            classFilesOutputDir,
            stubsOutputDir,
            stubsOutputDir // sic!
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
    val isVerbose: Boolean,
    val mapDiagnosticLocations: Boolean,
    val annotationProcessorFqNames: List<String>,
    val processorOptions: Map<String, String>,
    val javacOptions: Map<String, String>
) : Serializable

private data class KaptPathsForWorker(
    val projectBaseDir: File,
    val compileClasspath: List<File>,
    val annotationProcessingClasspath: List<File>,
    val javaSourceRoots: List<File>,
    val sourcesOutputDir: File,
    val classFilesOutputDir: File,
    val stubsOutputDir: File
) : Serializable