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
 * Loads the Build Tools API implementation from the directory passed by the IDE in [IMPL_HOME_PROPERTY].
 *
 * The implementation is loaded in an isolated class loader sharing only the API classes with the JPS plugin,
 * so the directory has to contain the whole implementation closure (`kotlin-compiler-embeddable` included).
 */
object JpsBtaToolchainLoader {
    const val IMPL_HOME_PROPERTY: String = "kotlin.jps.build.tools.impl.home"

    val implHomeProperty: String?
        get() = System.getProperty(IMPL_HOME_PROPERTY)

    val useBuildToolsApi: Boolean
        get() = !implHomeProperty.isNullOrEmpty()

    private val LOG = Logger.getInstance(JpsBtaToolchainLoader::class.java)

    private var cached: Pair<List<Path>, KotlinToolchains>? = null

    /**
     * @return the jars of [implDirectory], or `null` when there are none
     */
    fun resolveClasspath(implDirectory: String? = implHomeProperty): List<Path>? =
        File(implDirectory ?: return null)
            .listFiles()
            .orEmpty()
            .filter { it.isFile && it.name.endsWith(".jar") }
            .sortedBy { it.name }
            .map { it.toPath() }
            .takeIf { it.isNotEmpty() }

    /**
     * The loaded [KotlinToolchains] is cached for the lifetime of the build process, not per build:
     * `BuildSession.close()` does not release the class loader of the implementation.
     */
    @Synchronized
    fun load(): KotlinToolchains {
        val classpath = resolveClasspath()
            ?: error("No Build Tools API implementation: '$IMPL_HOME_PROPERTY' is not set, or its directory holds no jars")
        cached?.let { if (it.first == classpath) return it.second }

        val toolchains = KotlinToolchains.loadImplementation(classpath)
        reportVersions(toolchains, classpath)
        cached = classpath to toolchains
        return toolchains
    }

    /**
     * The implementation follows the Kotlin version of the project and may differ from the JPS plugin version.
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
