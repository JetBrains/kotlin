/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.jetbrains.kotlin.gradle.generators.native.cache.version.NativeCacheKotlinVersionsFile
import org.jetbrains.kotlin.gradle.generators.native.cache.version.NativeCacheKotlinVersionsGenerator
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.assertEquals

class GenerateKotlinVersionTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    @DisplayName("updateAndGetAll should add a new version to an existing file")
    fun `test NativeCacheKotlinVersionsFile - adds new version`() {
        val versionsFile = tempDir.resolve("native-cache-kotlin-versions.txt")
        versionsFile.writeText("v2_0_0\n")

        val versions = NativeCacheKotlinVersionsFile.updateAndGetAll(
            versionsFile,
            Triple(2, 1, 0),
        )

        assertEquals(
            setOf(Triple(2, 0, 0), Triple(2, 1, 0)),
            versions
        )
        assertEquals("v2_0_0\nv2_1_0", versionsFile.readText().trim())
    }

    @Test
    @DisplayName("updateAndGetAll should not rewrite file if version already exists")
    fun `test NativeCacheKotlinVersionsFile - does not add existing version`() {
        val versionsFile = tempDir.resolve("supported-kotlin-versions.txt")
        versionsFile.writeText("v2_0_0\nv2_1_0")
        val originalContent = versionsFile.readText()
        val lastModified = versionsFile.toFile().lastModified()

        val versions = NativeCacheKotlinVersionsFile.updateAndGetAll(
            versionsFile,
            Triple(2, 1, 0)
        )

        assertEquals(
            setOf(Triple(2, 0, 0), Triple(2, 1, 0)),
            versions
        )
        // File should not have been modified
        assertEquals(originalContent, versionsFile.readText())
        assertEquals(lastModified, versionsFile.toFile().lastModified())
    }

    @Test
    @DisplayName("updateAndGetAll should create a new file if one does not exist")
    fun `test NativeCacheKotlinVersionsFile - creates new file`() {
        val versionsFile = tempDir.resolve("supported-kotlin-versions.txt")

        val versions = NativeCacheKotlinVersionsFile.updateAndGetAll(
            versionsFile,
            Triple(2, 2, 0)
        )

        assertEquals(setOf(Triple(2, 2, 0)), versions)
        assertEquals("v2_2_0", versionsFile.readText().trim())
    }

    @Test
    @DisplayName("NativeCacheKotlinVersionsGenerator should generate the sealed class correctly")
    fun `test NativeCacheKotlinVersionsGenerator - generates correct code`() {
        val versions = setOf(
            Triple(1, 9, 0),
            Triple(1, 9, 255),
            Triple(1, 9, 2),
            Triple(2, 0, 0),
            Triple(2, 0, 20),
            Triple(2, 0, 255),
            Triple(2, 1, 0),
            Triple(2, 1, 255)
        )
        val (_, actualContent) = NativeCacheKotlinVersionsGenerator.generate(versions, true)

        // Use a multiline string to assert the exact file content is generated correctly.
        val expectedContent = """
            // This file was generated automatically. See the README.md file
            // DO NOT MODIFY IT MANUALLY.
            
            package org.jetbrains.kotlin.gradle.plugin.mpp
            
            import java.io.Serializable
            import kotlin.Comparable
            import kotlin.Deprecated
            import kotlin.DeprecationLevel
            import kotlin.Int
            import kotlin.String
            
            /**
             *
             * Provides type-safe constants for Kotlin versions to be used in the DSL for disabling the native cache.
             *
             * Disabling the native cache is not recommended and should only be used as a temporary workaround.
             * This class follows a rolling deprecation cycle to ensure that any cache-disabling configuration
             * is reviewed after a Kotlin update.
             *
             * Only the 3 most recent versions are included:
             * - **N (Latest):** The version constant is available.
             * - **N-1 (Deprecated):** The constant is marked with a deprecation warning.
             * - **N-2 (Error):** The constant is marked with a deprecation error.
             * - **N-3 (Dropped):** The constant is removed, causing a compilation failure.
             *
             * This forces a review of the cache-disabling configuration. If the problem is resolved,
             * please remove the DSL entry. If not, please update to the latest version constant.
             *
             * @since 2.3.20
             */
            @KotlinNativeCacheApi
            public sealed class DisableCacheInKotlinVersion private constructor(
              /**
               * The major version number.
               */
              public val major: Int,
              /**
               * The minor version number.
               */
              public val minor: Int,
              /**
               * The patch version number.
               */
              public val patch: Int,
            ) : Comparable<DisableCacheInKotlinVersion>,
                Serializable {
              /**
               * Returns the string representation of this version (e.g., 'v2_3_0').
               */
              override fun toString(): String = "v${'$'}{major}_${'$'}{minor}_${'$'}{patch}"
            
              /**
               * Compares this version to another version.
               */
              override fun compareTo(other: DisableCacheInKotlinVersion): Int = compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })
            
              /**
               * Represents the Kotlin version constant for 2.0.0.
               */
              @Deprecated(message = "Disabling native cache for this Kotlin version is no longer supported. Please update to the latest version constant or remove this DSL entry.", level = DeprecationLevel.ERROR)
              public object `2_0_0` : DisableCacheInKotlinVersion(2, 0, 0)
            
              /**
               * Represents the Kotlin version constant for 2.0.20.
               */
              @Deprecated(message = "Disabling native cache for this Kotlin version is deprecated. Please re-evaluate if this is still needed. If so, update to the latest version constant. If not, remove this DSL entry.")
              public object `2_0_20` : DisableCacheInKotlinVersion(2, 0, 20)
            
              /**
               * Represents the Kotlin version constant for 2.0.255.
               */
              public object `2_0_255` : DisableCacheInKotlinVersion(2, 0, 255)
            
              /**
               * Represents the Kotlin version constant for 2.1.0.
               */
              public object `2_1_0` : DisableCacheInKotlinVersion(2, 1, 0)
            
              /**
               * Represents the Kotlin version constant for 2.1.255.
               */
              public object `2_1_255` : DisableCacheInKotlinVersion(2, 1, 255)
            }
        
        """.trimIndent()

        // We must normalize line endings to '\n' (which KotlinPoet uses) to ensure
        // the test passes on Windows (which might use '\r\n').
        assertEquals(
            expectedContent.replace("\r\n", "\n"),
            actualContent.replace("\r\n", "\n")
        )
    }

    @Test
    @DisplayName("NativeCacheKotlinVersionsGenerator should generate the sealed class correctly")
    fun `test NativeCacheKotlinVersionsGenerator - generates without snapshots`() {
        val versions = setOf(
            Triple(2, 1, 0),
            Triple(2, 1, 255)
        )
        val (_, actualContent) = NativeCacheKotlinVersionsGenerator.generate(versions)


        // Use a multiline string to assert the exact file content is generated correctly.
        val expectedContent = """
            // This file was generated automatically. See the README.md file
            // DO NOT MODIFY IT MANUALLY.
            
            package org.jetbrains.kotlin.gradle.plugin.mpp
            
            import java.io.Serializable
            import kotlin.Comparable
            import kotlin.Int
            import kotlin.String
            
            /**
             *
             * Provides type-safe constants for Kotlin versions to be used in the DSL for disabling the native cache.
             *
             * Disabling the native cache is not recommended and should only be used as a temporary workaround.
             * This class follows a rolling deprecation cycle to ensure that any cache-disabling configuration
             * is reviewed after a Kotlin update.
             *
             * Only the 3 most recent versions are included:
             * - **N (Latest):** The version constant is available.
             * - **N-1 (Deprecated):** The constant is marked with a deprecation warning.
             * - **N-2 (Error):** The constant is marked with a deprecation error.
             * - **N-3 (Dropped):** The constant is removed, causing a compilation failure.
             *
             * This forces a review of the cache-disabling configuration. If the problem is resolved,
             * please remove the DSL entry. If not, please update to the latest version constant.
             *
             * @since 2.3.20
             */
            @KotlinNativeCacheApi
            public sealed class DisableCacheInKotlinVersion private constructor(
              /**
               * The major version number.
               */
              public val major: Int,
              /**
               * The minor version number.
               */
              public val minor: Int,
              /**
               * The patch version number.
               */
              public val patch: Int,
            ) : Comparable<DisableCacheInKotlinVersion>,
                Serializable {
              /**
               * Returns the string representation of this version (e.g., 'v2_3_0').
               */
              override fun toString(): String = "v${'$'}{major}_${'$'}{minor}_${'$'}{patch}"
   
              /**
               * Compares this version to another version.
               */
              override fun compareTo(other: DisableCacheInKotlinVersion): Int = compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })

              /**
               * Represents the Kotlin version constant for 2.1.0.
               */
              public object `2_1_0` : DisableCacheInKotlinVersion(2, 1, 0)
            }
        
        """.trimIndent()

        // We must normalize line endings to '\n' (which KotlinPoet uses) to ensure
        // the test passes on Windows (which might use '\r\n').
        assertEquals(
            expectedContent.replace("\r\n", "\n"),
            actualContent.replace("\r\n", "\n")
        )
    }
}