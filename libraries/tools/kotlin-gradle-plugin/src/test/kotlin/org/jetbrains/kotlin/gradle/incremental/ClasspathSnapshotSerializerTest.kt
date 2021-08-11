/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.incremental

import org.junit.Assert.*
import org.junit.Test

abstract class ClasspathSnapshotSerializerTest : ClasspathSnapshotTestCommon() {

    protected abstract val testSourceFile: ChangeableTestSourceFile

    @Test
    open fun `test ClassSnapshotDataSerializer`() {
        val originalSnapshot = testSourceFile.compileAndSnapshot()
        val serializedSnapshot = ClassSnapshotDataSerializer.toByteArray(originalSnapshot)
        val deserializedSnapshot = ClassSnapshotDataSerializer.fromByteArray(serializedSnapshot)

        assertEquals(originalSnapshot.toGson(), deserializedSnapshot.toGson())
    }
}

class KotlinClassesClasspathSnapshotSerializerTest : ClasspathSnapshotSerializerTest() {
    override val testSourceFile = SimpleKotlinClass(tmpDir)
}

class JavaClassesClasspathSnapshotSerializerTest : ClasspathSnapshotSerializerTest() {

    override val testSourceFile = SimpleJavaClass(tmpDir)

    @Test
    override fun `test ClassSnapshotDataSerializer`() {
        val originalSnapshot = testSourceFile.compileAndSnapshot()
        val serializedSnapshot = ClassSnapshotDataSerializer.toByteArray(originalSnapshot)
        val deserializedSnapshot = ClassSnapshotDataSerializer.fromByteArray(serializedSnapshot)

        // The deserialized object does not exactly match the original object as they contain fields called `memoizedSerializedSize` which
        // are not part of the serialized data and their values seem to be generated separately. Therefore, we remove these fields from the
        // check below.
        assertNotEquals(originalSnapshot.toGson(), deserializedSnapshot.toGson())
        assertEquals(
            originalSnapshot.toGson().stripLinesContainingText("memoizedSerializedSize"),
            deserializedSnapshot.toGson().stripLinesContainingText("memoizedSerializedSize")
        )
        // Add another check to confirm that those fields are not part of the serialized data.
        assertArrayEquals(serializedSnapshot, ClassSnapshotDataSerializer.toByteArray(deserializedSnapshot))
    }

    private fun String.stripLinesContainingText(text: String): String {
        return lines().filterNot { it.contains(text, ignoreCase = true) }.joinToString("\n")
    }
}
