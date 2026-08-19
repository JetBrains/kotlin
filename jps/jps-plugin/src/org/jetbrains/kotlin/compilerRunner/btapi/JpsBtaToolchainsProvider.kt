/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalBuildToolsApi::class)

package org.jetbrains.kotlin.compilerRunner.btapi

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.utils.KotlinPaths
import java.lang.ref.SoftReference
import java.nio.file.Path

/**
 * Resolves the Build Tools API implementation classpath from [KotlinPaths] and caches the loaded [KotlinToolchains].
 *
 * Caching matters because [KotlinToolchains.loadImplementation] builds a `URLClassLoader` over the whole compiler;
 * without it every build would construct a new one. This mirrors `CompilerRunnerUtil.ourClassLoaderRef` on the
 * legacy path.
 */
internal object JpsBtaToolchainsProvider {
    private var cache: SoftReference<Pair<List<Path>, KotlinToolchains>>? = null

    @Synchronized
    fun getToolchains(kotlinPaths: KotlinPaths, log: KotlinLogger): KotlinToolchains {
        val classpath = implementationClasspath(kotlinPaths, log)
        check(classpath.any { it.fileName.toString() == "${KotlinPaths.Jar.BuildToolsImpl.baseName}.jar" }) {
            "The Build Tools API implementation is not present in ${kotlinPaths.libPath}. " +
                    "Only a Kotlin distribution that ships ${KotlinPaths.Jar.BuildToolsImpl.baseName}.jar can be used " +
                    "with the Build Tools API path."
        }

        cache?.get()?.let { [cachedClasspath, cachedToolchains] ->
            if (cachedClasspath == classpath) return cachedToolchains
        }

        log.debug("Loading the Build Tools API implementation from ${classpath.joinToString()}")
        val toolchains = KotlinToolchains.loadImplementation(classpath)
        log.debug("Loaded the Build Tools API implementation of the compiler ${toolchains.getCompilerVersion()}")
        cache = SoftReference(classpath to toolchains)
        return toolchains
    }

    /**
     * Jars that are absent are dropped rather than passed to the classloader: the exact set a Kotlin distribution
     * ships varies, and only [KotlinPaths.Jar.BuildToolsImpl] is genuinely required, which is checked separately.
     *
     * The dropped ones are reported: the repo's `dist/kotlinc/lib` and the layout the JPS plugin sees in the IDE are
     * assembled by different builds (`prepare/compiler/build.gradle.kts` and `kotlin-dist-for-jps-meta`), so they can
     * disagree. Silently dropping a jar turns that disagreement into a `NoClassDefFoundError` from inside the
     * isolated classloader, which says nothing about which layout is short of what.
     */
    private fun implementationClasspath(kotlinPaths: KotlinPaths, log: KotlinLogger): List<Path> {
        val [present, missing] = KotlinPaths.ClassPaths.BuildToolsApi.contents
            .map { kotlinPaths.jar(it) }
            .partition { it.exists() }

        if (missing.isNotEmpty()) {
            log.warn(
                "The Build Tools API implementation classpath is missing ${missing.joinToString { it.name }} " +
                        "in ${kotlinPaths.libPath}"
            )
        }

        return present.map { it.toPath() } + annotationsJars(kotlinPaths)
    }

    /**
     * The JetBrains annotations jar is the one entry that carries its version in the file name
     * (`annotations-13.0.jar`), which [KotlinPaths.Jar] cannot express, so it is picked up by prefix instead.
     * The compiler needs it at code generation time, to emit `@NotNull` and `@Nullable`.
     */
    private fun annotationsJars(kotlinPaths: KotlinPaths): List<Path> =
        kotlinPaths.libPath
            .listFiles { file -> file.name.startsWith("annotations-") && file.name.endsWith(".jar") }
            ?.filterNot { it.name.endsWith("-sources.jar") }
            ?.map { it.toPath() }
            .orEmpty()
}
