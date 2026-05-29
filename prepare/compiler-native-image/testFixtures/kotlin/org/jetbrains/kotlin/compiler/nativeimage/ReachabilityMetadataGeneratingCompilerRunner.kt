/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.nativeimage

import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import java.io.File

class ReachabilityMetadataGeneratingCompilerRunner(private val javaHome: String) {
    private val javaExecutable: String by lazy {
        val binName = if (System.getProperty("os.name").startsWith("windows", ignoreCase = true)) "java.exe" else "java"
        File(javaHome, "bin").resolve(binName).absolutePath
    }

    private val embeddableClasspath: List<File> = ForTestCompileRuntime.kotlinCompilerEmbeddableClasspathForTests()

    private val kotlinHome: File = ForTestCompileRuntime.distKotlincForTests()

    private val reachabilityMetadataPath: String = ForTestCompileRuntime.kotlinNativeImageResourcesPathForTests()
        .resolve("META-INF/native-image/org/jetbrains/kotlin/kotlin-compiler-embeddable")
        .absolutePath

    fun run(
        workingDir: File,
        arguments: List<String>,
        classpath: List<File>,
        jvmArgs: List<String> = emptyList(),
    ): Pair<Int, String> {
        val cmd = buildList {
            add(javaExecutable)
            add("--add-opens"); add("java.base/java.lang=ALL-UNNAMED")
            add("--add-opens"); add("java.base/java.io=ALL-UNNAMED")
            add("--add-opens"); add("java.base/java.nio=ALL-UNNAMED")
            add("--add-opens"); add("java.base/sun.nio.ch=ALL-UNNAMED")
            add("--add-opens"); add("java.desktop/javax.swing=ALL-UNNAMED")
            add("-agentlib:native-image-agent=config-merge-dir=$reachabilityMetadataPath")
            add("-Djava.home=$javaHome")
            add("-Dkotlin.home=${kotlinHome.absolutePath}")
            // We simulate how the native image compiler runs, so we set this flag
            // Otherwise reachability metadata will contain plugins info which is not necessary
            add("-Dorg.graalvm.nativeimage.runtime=true")
            addAll(jvmArgs)
            add("-cp"); add(embeddableClasspath.joinToString(File.pathSeparator) { it.absolutePath })
            add("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")
            if (classpath.isNotEmpty()) {
                add("-cp")
                add(classpath.joinToString(File.pathSeparator))
            }
            addAll(arguments)
        }
        val process = ProcessBuilder(cmd)
            .directory(workingDir)
            .redirectErrorStream(true)
            .start()
        val out = process.inputStream.reader().use { it.readText() }
        return process.waitFor() to out
    }
}
