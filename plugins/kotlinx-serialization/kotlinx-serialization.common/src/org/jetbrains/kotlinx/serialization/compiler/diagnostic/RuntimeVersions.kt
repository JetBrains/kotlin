/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.diagnostic

import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import java.io.File
import java.util.jar.Attributes
import java.util.jar.JarFile


data class RuntimeVersions(val implementationVersion: ApiVersion?, val requireKotlinVersion: ApiVersion?) {
    companion object {
        val MINIMAL_SUPPORTED_VERSION = ApiVersion.parse("1.0-M1-SNAPSHOT")!!
        val MINIMAL_VERSION_FOR_INLINE_CLASSES = ApiVersion.parse("1.1-M1-SNAPSHOT")!!
    }

    fun currentCompilerMatchRequired(): Boolean {
        val current = requireNotNull(KotlinCompilerVersion.getVersion()?.let(ApiVersion.Companion::parse))
        return requireKotlinVersion == null || requireKotlinVersion <= current
    }

    fun implementationVersionMatchSupported(): Boolean {
        return implementationVersion != null && implementationVersion >= MINIMAL_SUPPORTED_VERSION
    }
}

object CommonVersionReader {
    private val REQUIRE_KOTLIN_VERSION = Attributes.Name("Require-Kotlin-Version")
    private const val CLASS_SUFFIX = "!/kotlinx/serialization/KSerializer.class"

    fun computeRuntimeVersions(sourceElement: SourceElement?): RuntimeVersions? {
        val location = (sourceElement as? KotlinJvmBinarySourceElement)?.binaryClass?.location ?: return null
        val jarFile = location.removeSuffix(CLASS_SUFFIX)
        if (!jarFile.endsWith(".jar")) return null
        val file = File(jarFile)
        if (!file.exists()) return null
        return getVersionsFromManifest(file)
    }

    fun getVersionsFromManifest(runtimeLibraryPath: File): RuntimeVersions {
        val version = getJarAttribute(runtimeLibraryPath, Attributes.Name.IMPLEMENTATION_VERSION)?.let(ApiVersion.Companion::parse)
        val kotlinVersion = getJarAttribute(runtimeLibraryPath, REQUIRE_KOTLIN_VERSION)?.let(ApiVersion.Companion::parse)
        return RuntimeVersions(version, kotlinVersion)
    }

    fun canSupportInlineClasses(currentVersion: RuntimeVersions?): Boolean {
        if (currentVersion == null) return true
        val implVersion = currentVersion.implementationVersion ?: return false
        return implVersion >= RuntimeVersions.MINIMAL_VERSION_FOR_INLINE_CLASSES
    }

    private fun getJarAttribute(file: File, attribute: Attributes.Name): String? {
        if (file.canRead()) {
            JarFile(file).use { jarFile ->
                return jarFile.manifest?.mainAttributes?.getValue(attribute)
            }
        }
        return null
    }
}
