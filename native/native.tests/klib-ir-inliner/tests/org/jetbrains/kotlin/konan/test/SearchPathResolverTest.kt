/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test

import org.jetbrains.kotlin.konan.library.KonanLibraryProperResolver
import org.jetbrains.kotlin.konan.library.impl.buildLibrary
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KLIB_PROPERTY_COMMONIZER_NATIVE_TARGETS
import org.jetbrains.kotlin.library.KLIB_PROPERTY_NATIVE_TARGETS
import org.jetbrains.kotlin.library.KotlinLibraryVersioning
import org.jetbrains.kotlin.library.LenientUnresolvedLibrary
import org.jetbrains.kotlin.library.RequiredUnresolvedLibrary
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEquals
import org.jetbrains.kotlin.util.DummyLogger
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.Properties
import java.util.UUID
import kotlin.random.Random

class SearchPathResolverTest {
    @TempDir
    lateinit var tmpDir: File

    @Test
    fun testResolveByTargetInNativeTargetsProperty() = doTest(KLIB_PROPERTY_NATIVE_TARGETS)

    @Test
    fun testResolveByTargetInCommonizedNativeTargetsProperty() = doTest(KLIB_PROPERTY_COMMONIZER_NATIVE_TARGETS)

    private fun doTest(targetPropertyName: String) {
        KonanTarget.predefinedTargets.values.forEach { currentTarget ->
            val resolver = KonanLibraryProperResolver(
                directLibs = emptyList(),
                target = currentTarget,
                distributionKlib = null,
                skipCurrentDir = true,
                logger = DummyLogger,
            )

            val libraryWithTarget = generateKlib(
                mainTarget = currentTarget,
                targetPropertyName = targetPropertyName,
                targetList = randomTargetsIncluding(5, currentTarget)
            )
            resolver.resolve(RequiredUnresolvedLibrary(libraryWithTarget))

            val libraryWithoutTarget = generateKlib(
                mainTarget = currentTarget,
                targetPropertyName = targetPropertyName,
                targetList = randomTargetsExcluding(5, currentTarget)
            )
            assertEquals(null, resolver.resolve(LenientUnresolvedLibrary(libraryWithoutTarget)))
        }
    }

    private fun generateKlib(
        mainTarget: KonanTarget,
        targetPropertyName: String,
        targetList: List<KonanTarget>,
    ): String {
        val outputDir = tmpDir.resolve(UUID.randomUUID().toString()).apply(File::mkdirs).absolutePath

        buildLibrary(
            natives = emptyList(),
            included = emptyList(),
            linkDependencies = emptyList(),
            metadata = DUMMY_METADATA,
            ir = null,
            versions = DUMMY_VERSIONS,
            target = mainTarget,
            output = outputDir,
            moduleName = "test",
            nopack = true,
            shortName = "test",
            manifestProperties = Properties().apply { setProperty(targetPropertyName, targetList.joinToString(" ")) }
        )

        return outputDir
    }

    companion object {
        private val DUMMY_VERSIONS = KotlinLibraryVersioning(null, null, null)
        private val DUMMY_METADATA = SerializedMetadata(byteArrayOf(), emptyList(), emptyList())

        private fun randomTargetsIncluding(number: Int, requiredTarget: KonanTarget): List<KonanTarget> {
            require(number >= 1)

            val result: HashSet<KonanTarget> = hashSetOf(requiredTarget)
            while (result.size < number) {
                result += randomTarget()
            }

            return result.sortedBy { it.name }
        }

        private fun randomTargetsExcluding(number: Int, undesiredTarget: KonanTarget): List<KonanTarget> {
            require(number >= 0)

            val result: HashSet<KonanTarget> = hashSetOf()
            while (result.size < number) {
                randomTarget().takeIf { it != undesiredTarget }?.let { result += it }
            }

            return result.sortedBy { it.name }
        }

        private fun randomTarget() = KonanTarget.predefinedTargets.values.random(Random(System.nanoTime()))
    }
}