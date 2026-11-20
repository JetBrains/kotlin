/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.nativeimage

import org.jetbrains.kotlin.cli.common.isWindows
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.daemon.common.CompilerId
import java.io.File

/**
 * Runs the JVM compiler daemon with the tracing agent to collect reachability metadata for daemon compilation.
 */
class TracingAgentDaemonSession(javaHome: String = System.getProperty("java.home")) : DaemonSession(javaHome) {
    override val executable: String by lazy {
        File(javaHome, "bin").resolve(if (isWindows) "java.exe" else "java").absolutePath
    }

    private val compilerClasspath = ForTestCompileRuntime.kotlinCompilerEmbeddableClasspathForTests() +
            ForTestCompileRuntime.kotlinNativeImageResourcesPathForTests()
    override val compilerId = CompilerId.makeCompilerId(compilerClasspath)

    override val jvmArgs: List<String> by lazy {
        buildList {
            addAll(TracingAgentCompilerRunner.agentArgs())
            addAll(super.jvmArgs)
            addAll(compilerClasspath.asClasspath())
            add("org.jetbrains.kotlin.daemon.KotlinCompileDaemon")
        }
    }
}
