/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner.btapi

import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import java.io.File
import java.nio.file.Path

/**
 * Loads the Build Tools API implementation through [KotlinToolchains.loadImplementation], which puts it behind a
 * `URLClassLoader` sharing only the `org.jetbrains.kotlin.buildtools.api` packages with the JPS plugin's own
 * loader. Everything else resolves against the JDK platform loader, so the classpath has to be self-contained.
 *
 * The implementation is version-coupled to the compiler, so it is the IDE that resolves it for the project's
 * configured Kotlin version and hands the directory over in [IMPL_HOME_PROPERTY]. The `kotlinc` dist does not
 * ship it, and deliberately so: it would mean a second copy of the compiler in every IDE installation.
 *
 * Note that this is not the class loader [org.jetbrains.kotlin.compilerRunner.CompilerRunnerUtil] builds for the
 * legacy path: that one holds the unrelocated `kotlin-compiler.jar`, while the implementation is compiled against
 * the relocated `kotlin-compiler-embeddable.jar`.
 */
object JpsBtaToolchainLoader {
    /**
     * Directory holding the implementation closure, handed over by the IDE through
     * `BuildProcessParametersProvider.getPathParameters()` - a directory rather than a path-separated classpath
     * because only path parameters are translated for a build process running on another host.
     */
    const val IMPL_HOME_PROPERTY: String = "kotlin.jps.btaImplHome"

    private val LOG = Logger.getInstance(JpsBtaToolchainLoader::class.java)

    private var cached: Pair<List<Path>, KotlinToolchains>? = null

    /**
     * @return the implementation classpath, or `null` when the IDE handed over nothing usable - a reason to stay
     * on the legacy path, not to fail the build.
     */
    fun resolveClasspath(implDirectory: String? = System.getProperty(IMPL_HOME_PROPERTY)): List<Path>? =
        File(implDirectory ?: return null)
            .listFiles()
            .orEmpty()
            .filter { it.isFile && it.name.endsWith(".jar") }
            .sortedBy { it.name }
            .map { it.toPath() }
            .takeIf { it.isNotEmpty() }

    /**
     * The loaded [KotlinToolchains] is cached for the lifetime of the build process rather than per build:
     * `BuildSession.close()` does not release the `URLClassLoader`, and re-creating it over the embeddable
     * compiler on every build would be wasteful.
     */
    @Synchronized
    fun load(): KotlinToolchains? {
        val classpath = resolveClasspath()
        if (classpath == null) {
            LOG.info("No Build Tools API implementation: '$IMPL_HOME_PROPERTY' is not set, or its directory holds no jars")
            return null
        }
        cached?.let { if (it.first == classpath) return it.second }

        val toolchains = KotlinToolchains.loadImplementation(classpath)
        reportVersions(toolchains, classpath)
        cached = classpath to toolchains
        return toolchains
    }

    /**
     * The implementation follows the Kotlin version of the project, and the API follows the JPS plugin. A build
     * that uses two different versions stays inside the compatibility range of the API. Report both versions, so
     * that a difference is visible in the build log.
     */
    private fun reportVersions(toolchains: KotlinToolchains, classpath: List<Path>) {
        val implementationVersion = toolchains.getCompilerVersion()
        val jpsPluginVersion = KotlinCompilerVersion.VERSION
        LOG.info(
            "Loaded Build Tools API implementation $implementationVersion from ${classpath.size} jars" +
                    " (API ${KotlinToolchains.getVersion()}, JPS plugin $jpsPluginVersion)"
        )
        if (implementationVersion != jpsPluginVersion) {
            LOG.warn(
                "The Build Tools API implementation is $implementationVersion, but the JPS plugin is" +
                        " $jpsPluginVersion. The build uses the implementation of the project."
            )
        }
    }
}
