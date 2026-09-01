/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner.btapi

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import java.io.File
import java.lang.ref.SoftReference
import java.nio.file.Path

/**
 * Loads the Build Tools API implementation out of the `kotlinc/lib` directory that `jps.kotlin.home` points at.
 *
 * The implementation is loaded through [KotlinToolchains.loadImplementation], which puts it behind a
 * `URLClassLoader` whose parent shares only the `org.jetbrains.kotlin.buildtools.api` packages. Everything else
 * resolves against the JDK platform loader, so the classpath handed to it has to be self-contained.
 *
 * This is deliberately *not* the class loader that [org.jetbrains.kotlin.compilerRunner.CompilerRunnerUtil] builds
 * for the legacy path: that one holds the unrelocated `kotlin-compiler.jar`, while the Build Tools API
 * implementation is compiled against the relocated `kotlin-compiler-embeddable.jar`.
 */
internal object JpsBtaToolchainLoader {
    /**
     * The runtime closure of `:compiler:build-tools:kotlin-build-tools-impl`, as declared by its build script.
     *
     * Kept in sync with `distBuildToolsApiImplProjects` in `prepare/compiler/build.gradle.kts`.
     */
    private val IMPL_CLOSURE_JARS = listOf(
        "kotlin-build-tools-impl.jar",
        "kotlin-build-tools-cri-impl.jar",
        "kotlin-compiler-embeddable.jar",
        "kotlin-compiler-runner.jar",
        "kotlin-tooling-core.jar",
        "kotlin-stdlib.jar",
        // `compileOnly` in the implementation's build script, but needed at run time: `argumentUtils.getUsingReflection`
        // goes through `kotlin.reflect.full`.
        "kotlin-reflect.jar",
        // Also `compileOnly` there, and also needed at run time even for in-process execution:
        // `JvmCompilationOperationImpl.targetPlatform` is a `daemon-common` `CompileService.TargetPlatform`.
        // `:kotlin-compiler-runner` declares `api(project(":kotlin-daemon-client"))` anyway.
        "kotlin-daemon-client.jar",
        // The rest of `:kotlin-compiler-embeddable`'s own runtime closure.
        "kotlinx-coroutines-core-jvm.jar",
        "kotlin-script-runtime.jar",
        "annotations-13.0.jar",
    )

    /**
     * Set to `true` to ignore [IMPL_CLOSURE_JARS] and use every jar in `lib` instead. An escape hatch for the case
     * where the hand-maintained closure above has drifted and a `NoClassDefFoundError` shows up.
     */
    private const val USE_WHOLE_LIB_PROPERTY = "kotlin.jps.btaUseWholeLibDirectory"

    private var cache: SoftReference<Pair<List<Path>, KotlinToolchains>> = SoftReference(null)

    @Synchronized
    fun load(libDirectory: File): KotlinToolchains {
        val classpath = implementationClasspath(libDirectory)
        cache.get()?.let { cached ->
            if (cached.first == classpath) return cached.second
        }

        val toolchains = KotlinToolchains.loadImplementation(classpath)
        cache = SoftReference(classpath to toolchains)
        return toolchains
    }

    private fun implementationClasspath(libDirectory: File): List<Path> {
        if (!System.getProperty(USE_WHOLE_LIB_PROPERTY, "false").toBoolean()) {
            val declared = IMPL_CLOSURE_JARS.map { File(libDirectory, it) }
            if (declared.all { it.isFile }) return declared.map { it.toPath() }
        }

        // `kotlin-compiler.jar` is the unrelocated compiler: putting it next to `kotlin-compiler-embeddable.jar`
        // would give the implementation two copies of every compiler class.
        return (libDirectory.listFiles() ?: emptyArray())
            .filter { it.isFile && it.name.endsWith(".jar") && it.name != "kotlin-compiler.jar" && !it.name.endsWith("-sources.jar") }
            .sortedBy { it.name }
            .map { it.toPath() }
    }
}
