/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.modelx.plainBuildSystem

import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.project.modelx.*
import org.jetbrains.kotlin.project.modelx.serialization.JsonKpmSerializer
import org.jetbrains.kotlin.project.modelx.serialization.KotlinModuleDtoTransformer
import org.jetbrains.kotlin.project.modelx.serialization.KpmSerializer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Comparator
import java.util.jar.JarFile
import kotlin.io.path.*

class PlainModuleDependencyProvider(
    private val kpmSerializer: KpmSerializer,
    private val dtoTransformer: KotlinModuleDtoTransformer,
    private val basePathForMedatada: Path
) {
    private val modules = mutableMapOf<ModuleId, KotlinModuleDependency>()

    fun scanForDefaultLibs(libsDir: Path) {
        scanForStdlib(libsDir)
        scanForReflectionLib(libsDir)
    }

    fun scanForStdlib(libsDir: Path) {
        val commonStdLib = libsDir.resolve("kotlin-stdlib-common.jar")
        val jdkStdLib = libsDir.resolve("kotlin-stdlib.jar")
        val jdk7StdLib = libsDir.resolve("kotlin-stdlib-jdk7.jar")
        val jdk8StdLib = libsDir.resolve("kotlin-stdlib-jdk8.jar")
        val jsStdLib = libsDir.resolve("kotlin-stdlib-js.jar")

        val jsPresent = jsStdLib.exists()
        val jvmPresent = jdkStdLib.exists()

        val fragments = listOfNotNull(
            CommonFragment(id = "common", settings = emptyMap(), moduleDependencies = emptySet()),
            Variant(
                id = "jvm",
                settings = emptyMap(),
                attributes = mapOf(Platforms to Platforms(Platform.JVM), JvmTargetAttribute to JvmTargetAttribute(JvmTarget.JVM_1_8)),
                moduleDependencies = emptySet()
            ).takeIf { jvmPresent },
            Variant(
                id = "js",
                settings = emptyMap(),
                attributes = mapOf(Platforms to Platforms(Platform.JS)),
                moduleDependencies = emptySet()
            ).takeIf { jsPresent }
        ).associateBy { it.id }

        val module = KotlinModule(
            id = "stdlib",
            fragments = fragments,
            refinements = fragments
                .values
                .filterIsInstance<Variant>()
                .associate { it.id to setOf("common") } // all variants refine just common
        )

        val moduleDependency = KotlinModuleDependency(
            fragmentArtifacts = fragments.mapValues { listOf(commonStdLib) },
            variantArtifacts = listOfNotNull(
                if (jsPresent) "js" to listOf(commonStdLib, jsStdLib) else null,
                if (jvmPresent) "jvm" to listOf(commonStdLib, jdkStdLib, jdk7StdLib, jdk8StdLib).filter { it.exists() } else null
            ).toMap(),
            module = module
        )

        if (moduleDependency.variantArtifacts.isNotEmpty()) {
            // TODO: stdlib may already exists, need to decide what to do replace or merge?
            modules["stdlib"] = moduleDependency
        }
    }

    fun scanForReflectionLib(libsDir: Path) {
        // TODO:
    }

    fun scan(libsDir: Path) {
        val session = ModulesScanSession()

        libsDir
            .listDirectoryEntries("*.jar")
            .forEach { session.addJar(it) }

        modules += session.moduleDependencies()
    }

    fun moduleDependencies(): Map<ModuleId, KotlinModuleDependency> = modules.toMap()

    fun moduleDependency(id: ModuleId): KotlinModuleDependency? = modules[id]

    private data class MetadataArtifact(
        val artifact: Path,
        val module: KotlinModule
    )

    private inner class ModulesScanSession {
        val metadataArtifacts = mutableMapOf<ModuleId, MetadataArtifact>()
        val variantArtifacts = mutableMapOf<ModuleId, MutableMap<FragmentId, Path>>()

        fun addJar(jar: Path): Boolean = when {
            jar.isMetadata -> addMetadataJar(jar)
            jar.isVariant -> addVariantJar(jar)
            else -> false // do nothing when
        }

        private val Path.isMetadata
            get(): Boolean {
                val jar = JarFile(toFile())
                return jar.getJarEntry("META-INF/kpm.json") != null
            }

        private val Path.isVariant
            get(): Boolean {
                val jar = JarFile(toFile())
                return jar.getJarEntry("META-INF/kpm-variant.json") != null
            }

        fun addMetadataJar(path: Path): Boolean {
            val jar = JarFile(path.toFile())
            val metadataJsonEntry = jar.getJarEntry("META-INF/kpm.json") ?: return false

            val moduleDto = kpmSerializer.deserialize(jar.getInputStream(metadataJsonEntry).readBytes())
            val module = dtoTransformer.fromDto(moduleDto)

            val id = module.id

            metadataArtifacts[id] = MetadataArtifact(path, module)
            return true
        }

        fun addVariantJar(path: Path): Boolean {
            val jar = JarFile(path.toFile())
            val jsonEntry = jar.getJarEntry("META-INF/kpm-variant.json") ?: return false

            val variantInfo = kpmSerializer.deserializeVariantInfo(jar.getInputStream(jsonEntry).readBytes())
            val moduleId = variantInfo.moduleId
            val variantId = variantInfo.variant.id

            variantArtifacts
                .getOrPut(moduleId) { mutableMapOf() }
                .put(variantId, path)

            return true
        }

        fun moduleDependencies(): MutableMap<ModuleId, KotlinModuleDependency> {
            val result = mutableMapOf<ModuleId, KotlinModuleDependency>()

            for ((moduleId, metadata) in metadataArtifacts) {
                val fragmentArtifacts = extractFragmentArtifacts(moduleId, metadata.artifact)
                val variants = variantArtifacts[moduleId] ?: emptyMap()

                result[moduleId] = KotlinModuleDependency(
                    fragmentArtifacts = fragmentArtifacts,
                    variantArtifacts = variants.mapValues { listOf(it.value) /*TODO: + its dependencies, but how? */ },
                    module = metadata.module
                )
            }

            return result
        }

        private fun extractFragmentArtifacts(moduleId: ModuleId, jarPath: Path): Map<FragmentId, List<Path>> {
            val destination = basePathForMedatada.resolve(moduleId)
            // val result = mutableMapOf<FragmentId, List<Path>>()

            // Clean up first
            if (destination.exists()) {
                Files.walk(destination)
                    .sorted(Comparator.reverseOrder())
                    .forEach(Path::deleteIfExists)
            } else {
                destination.createDirectories()
            }

            // Now extract
            val jar = JarFile(jarPath.toFile())
            jar
                .entries()
                .asSequence()
                .onEach { println(it.name) }
                .filterNot { it.name.startsWith("META-INF") }
                .forEach { entry ->
                    val filePath = destination.resolve(entry.name)
                    if (entry.isDirectory) {
                        filePath.createDirectories()
                    } else {
                        filePath.writeBytes(jar.getInputStream(entry).readBytes())
                    }
                }

            // FIXME: jar can contain extra directories that can break this logic.
            // Instead it is better to load kpm.json and extract all the fragments from there
            return destination
                .listDirectoryEntries()
                .associate { it.name to listOf(it) }
        }
    }
}