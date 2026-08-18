/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.json

import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class AbsoluteFileSerializerTest {

    @Test
    fun `serializes a file as its absolute path`(@TempDir tempDir: File) {
        val file = tempDir.resolve("some.txt")
        // Compared against the plain string encoding of the absolute path, so escaping and the
        // path separator stay platform-independent.
        val expected = KgpJson.default.encodeToString(String.serializer(), file.absolutePath)
        assertEquals(expected, KgpJson.default.encodeToString(AbsoluteFileSerializer, file))
    }

    @Test
    fun `serializes a relative file using its absolute path`() {
        val relative = File("relative/path")
        val json = KgpJson.default.encodeToString(AbsoluteFileSerializer, relative)

        assertEquals(relative.absoluteFile, KgpJson.default.decodeFromString(AbsoluteFileSerializer, json))
        assertFalse(relative.isAbsolute, "precondition: the input file must be relative")
    }

    @Test
    fun `round-trips an absolute file`(@TempDir tempDir: File) {
        val file = tempDir.resolve("nested/output.json").absoluteFile
        val json = KgpJson.default.encodeToString(AbsoluteFileSerializer, file)
        assertEquals(file, KgpJson.default.decodeFromString(AbsoluteFileSerializer, json))
    }

    @Test
    fun `deserializes a path string back into a file`(@TempDir tempDir: File) {
        val path = tempDir.resolve("artifact.klib").absolutePath
        val json = KgpJson.default.encodeToString(String.serializer(), path)
        assertEquals(File(path), KgpJson.default.decodeFromString(AbsoluteFileSerializer, json))
    }

    @Test
    fun `round-trips a map of files`(@TempDir tempDir: File) {
        val serializer = MapSerializer(String.serializer(), AbsoluteFileSerializer)
        val map = mapOf(
            "commonMain" to tempDir.resolve("commonMain").absoluteFile,
            "jvmMain" to tempDir.resolve("jvmMain").absoluteFile,
        )
        val json = KgpJson.default.encodeToString(serializer, map)
        assertEquals(map, KgpJson.default.decodeFromString(serializer, json))
    }
}
