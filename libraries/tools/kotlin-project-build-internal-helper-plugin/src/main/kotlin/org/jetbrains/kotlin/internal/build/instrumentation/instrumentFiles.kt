/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.internal.build.instrumentation

import com.intellij.compiler.instrumentation.FailSafeClassReader
import com.intellij.compiler.instrumentation.InstrumentationClassFinder
import com.intellij.compiler.instrumentation.InstrumenterClassWriter
import com.intellij.compiler.notNullVerification.NotNullVerifyingInstrumenter
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.org.objectweb.asm.ClassWriter
import java.io.File
import java.net.URL
import java.nio.file.Files
import kotlin.collections.ArrayList

private val notNullAnnotations = arrayOf("org.jetbrains.annotations.NotNull")

internal fun addNotNullVerification(
    project: Project,
    filesToProcess: Set<File>,
    classpath: Iterable<File>,
    javaHome: File
) {
    val instrumentationArgs = InstrumentationArgs(
        filesToProcess = filesToProcess.toList(), classpathFiles = classpath.toList(), javaHome = javaHome
    )

    if (JavaRuntimeURLs.hasJrt(javaHome) && JavaVersion.current() < JavaVersion.VERSION_1_9) {
        instrumentOutOfProcess(instrumentationArgs, project)
    } else {
        instrumentInProcess(instrumentationArgs)
    }
}

private fun instrumentOutOfProcess(
    instrumentationArgs: InstrumentationArgs,
    project: Project
) {
    val argFile = Files.createTempFile("instrumentation-args", ".bin").toFile()
    try {
        InstrumentationArgs.writeToFile(instrumentationArgs, argFile)
        project.javaexec { spec ->
            val executableName = if (OperatingSystem.current().isWindows) "java.exe" else "java"
            spec.executable = instrumentationArgs.javaHome.resolve("bin/$executableName").path
            spec.classpath = project.files(PluginClasspath.get(project))
            spec.main = NotNullInstrumentationCliFacade::class.java.canonicalName
            spec.args = listOf(argFile.absolutePath)
        }
    } finally {
        argFile.delete()
    }
}

object NotNullInstrumentationCliFacade {
    @JvmStatic
    fun main(args: Array<String>) {
        val argFile = File(args.single())
        val instrumentationArgs = InstrumentationArgs.readFromFile(argFile)
        instrumentInProcess(instrumentationArgs)
    }
}

private fun instrumentInProcess(args: InstrumentationArgs) {
    val classpathURLs = ArrayList<URL>()
    args.classpathFiles.mapTo(classpathURLs) { it.toURI().toURL() }
    classpathURLs.addAll(JavaRuntimeURLs.get(args.javaHome))

    val inputBytes = args.filesToProcess.associateWith { it.readBytes() }
    val finder = InstrumentationClassFinder(classpathURLs.toTypedArray())
    val outputBytes = args.filesToProcess.associateWith {
        val classReader = FailSafeClassReader(inputBytes[it])
        val classWriter = InstrumenterClassWriter(ClassWriter.COMPUTE_FRAMES, finder)
        NotNullVerifyingInstrumenter.processClassFile(classReader, classWriter, notNullAnnotations)
        classWriter.toByteArray()
    }
    for ((file, bytes) in outputBytes) {
        file.writeBytes(bytes)
    }
}
