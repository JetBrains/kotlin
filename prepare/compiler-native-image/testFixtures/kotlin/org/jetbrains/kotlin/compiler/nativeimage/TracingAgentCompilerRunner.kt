/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.nativeimage

import org.jetbrains.kotlin.cli.common.isWindows
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import java.io.File

class TracingAgentCompilerRunner(javaHome: String = System.getProperty("java.home")) : SubprocessCompilerRunner(javaHome) {
    override val executable: String by lazy {
        File(javaHome, "bin").resolve(if (isWindows) "java.exe" else "java").absolutePath
    }

    override val jvmArgs: List<String> by lazy {
        buildList {
            val embeddableClasspath = ForTestCompileRuntime.kotlinCompilerEmbeddableClasspathForTests()
            val kotlinHome = ForTestCompileRuntime.distKotlincForTests()
            addAll(agentArgs())
            add("-Djava.home=$javaHome")
            add("-Dkotlin.home=${kotlinHome.absolutePath}")
            addAll(DEFAULT_JVM_ARGS)
            addAll(embeddableClasspath.asClasspath())
            add(COMPILER_MAIN_CLASS)
        }
    }

    companion object {
        private const val COMPILER_MAIN_CLASS = "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler"

        internal fun agentArgs(): List<String> = buildList {
            val reachabilityMetadataPath = ForTestCompileRuntime.kotlinNativeImageResourcesPathForTests()
                .resolve("META-INF/native-image/org/jetbrains/kotlin/kotlin-compiler-embeddable")
            ADD_OPENS.forEach { add("--add-opens"); add(it) }
            add("-agentlib:native-image-agent=config-merge-dir=${reachabilityMetadataPath.absolutePath}")
            // Simulate the native image runtime environment
            add("-Dorg.graalvm.nativeimage.imagecode=runtime")
        }

        private val ADD_OPENS = listOf(
            "java.base/java.lang=ALL-UNNAMED",
            "java.base/java.io=ALL-UNNAMED",
            "java.base/java.nio=ALL-UNNAMED",
            "java.base/sun.nio.ch=ALL-UNNAMED",
            "java.desktop/javax.swing=ALL-UNNAMED",
        )
    }
}
