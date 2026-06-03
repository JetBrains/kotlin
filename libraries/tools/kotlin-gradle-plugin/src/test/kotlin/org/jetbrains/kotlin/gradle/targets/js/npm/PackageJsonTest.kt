/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.lang.reflect.Modifier
import kotlin.test.assertNull
import java.nio.file.FileSystemException

class PackageJsonTest {

    @Test
    fun `saveTo non-existing directory fails`(@TempDir tempDir: File) {
        val blockedParent = tempDir.resolve("blocked")
        // Create a file instead of a directory, so createDirectories() will fail
        blockedParent.createNewFile()

        val target = blockedParent.resolve("nested/package.json")

        val pj = PackageJson(name = "foo", version = "1.0.0")

        assertThrows<FileSystemException> {
            pj.saveTo(target)
        }

        assertFalse(target.parentFile.exists(), "Expect parent directory was not created.")
    }

    @Test
    fun `saveTo creates missing parent directories`(@TempDir tempDir: File) {
        val target = tempDir.resolve("a/b/c/package.json")

        PackageJson(name = "foo", version = "1.0.0").saveTo(target)

        assertTrue(target.isFile, "Expected $target to be a file.")
        val parsed = fromSrcPackageJson(target)
        assertEquals("foo", parsed?.name)
    }

    @Test
    fun `saveTo rewrites an unparseable existing file`(@TempDir tempDir: File) {
        val target = tempDir.resolve("package.json")
        // what an interrupted write leaves behind
        target.writeText("{\"name\": \"fo")

        PackageJson(name = "foo", version = "1.0.0").saveTo(target)

        assertEquals("foo", fromSrcPackageJson(target)?.name)
    }

    @Test
    fun `saveTo reads back a file with a byte order mark`(@TempDir tempDir: File) {
        val target = tempDir.resolve("package.json")
        PackageJson(name = "foo", version = "1.0.0").saveTo(target)
        target.writeText("\uFEFF" + target.readText())
        val stamp = target.lastModified()

        PackageJson(name = "foo", version = "1.0.0").saveTo(target)

        assertEquals(stamp, target.lastModified(), "Expected an unchanged package.json to not be rewritten.")
    }

    @Test
    fun `a package json without a name is ignored`(@TempDir tempDir: File) {
        val target = tempDir.resolve("package.json")
        target.writeText("{\"version\": \"1.0.0\"}")

        assertNull(fromSrcPackageJson(target))
    }

    @Test
    fun `a package json with comments is read, as Gson did`(@TempDir tempDir: File) {
        val target = tempDir.resolve("package.json")
        target.writeText(
            """
            {
              // the main entry
              "name": "commented", /* no version */
              "main": "index.js"
            }
            """.trimIndent()
        )

        assertEquals("commented", fromSrcPackageJson(target)?.name)
    }

    @Test
    fun `moduleName falls back to the directory name`(@TempDir tempDir: File) {
        val directory = tempDir.resolve("some-module").also { it.mkdirs() }
        // a private package may legitimately have no "name"
        directory.resolve("package.json").writeText("{\"private\": true}")

        assertEquals("some-module", moduleName(directory))
    }

    private data class Author(val name: String, val email: String?)

    @Test
    fun `customField keeps nulls, as the Gson writer for package json did`(@TempDir tempDir: File) {
        val target = tempDir.resolve("package.json")
        PackageJson(name = "foo", version = "1.0.0")
            .apply { customField("author", mapOf("name" to "Jane", "email" to null)) }
            .saveTo(target)

        assertEquals(
            buildJsonObject {
                put("name", "Jane")
                put("email", JsonNull)
            },
            parsePackageJsonObject(target)["author"],
        )
    }

    @Test
    fun `customField rejects a plain object`(@TempDir tempDir: File) {
        val packageJson = PackageJson(name = "foo", version = "1.0.0").apply { customField("author", Author("Jane", email = null)) }

        assertThrows<IllegalStateException> { packageJson.saveTo(tempDir.resolve("package.json")) }
    }

    @Test
    fun `a missing version falls back to the given default`(@TempDir tempDir: File) {
        val target = tempDir.resolve("package.json")
        target.writeText("{\"name\": \"foo\"}")

        assertEquals("2.0.0", parsePackageJson(target, defaultVersion = "2.0.0")?.version)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `PackageJsonFile declares every PackageJson property`() {
        // Gson picked up a new PackageJson property by reflection; the DTO has to be told about it
        val properties = PackageJson::class.java.declaredFields
            .filterNot { Modifier.isStatic(it.modifiers) }
            .map { it.name }
            .toSet() - "customFields"
        val elements = PackageJsonFile.serializer().descriptor.elementNames.toSet()

        assertEquals(properties, elements)
    }

    @Test
    fun `customField writes a Char as a string`(@TempDir tempDir: File) {
        val target = tempDir.resolve("package.json")
        PackageJson(name = "foo", version = "1.0.0").apply { customField("separator", '/') }.saveTo(target)

        assertEquals(JsonPrimitive("/"), parsePackageJsonObject(target)["separator"])
    }

    @Test
    fun `a dependency package json without a name keeps its dependencies`(@TempDir tempDir: File) {
        val target = tempDir.resolve("package.json")
        target.writeText("{\"main\": \"lib.js\", \"dependencies\": {\"left-pad\": \"1.3.0\"}}")

        val parsed = parsePackageJson(target, defaultName = "my-module")

        assertEquals("my-module", parsed?.name)
        assertEquals("lib.js", parsed?.main)
        assertEquals(mapOf("left-pad" to "1.3.0"), parsed?.dependencies)
    }

    @Test
    fun `a malformed dependency package json fails`(@TempDir tempDir: File) {
        val target = tempDir.resolve("package.json")
        target.writeText("{\"name\": \"fo")

        assertThrows<IllegalArgumentException> { parsePackageJson(target, defaultName = "my-module") }
        assertNull(fromSrcPackageJson(target))
    }
}
