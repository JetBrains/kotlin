/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.incremental

import org.junit.Assert.assertEquals
import org.junit.Test

abstract class ClasspathSnapshotSerializerTest : ClasspathSnapshotTestCommon() {

    protected abstract val testSourceFile: ChangeableTestSourceFile

    @Test
    fun `test ClassSnapshotDataSerializer`() {
        val originalSnapshot = testSourceFile.compileAndSnapshot()
        val serializedSnapshot = ClassSnapshotDataSerializer.toByteArray(originalSnapshot)
        val deserializedSnapshot = ClassSnapshotDataSerializer.fromByteArray(serializedSnapshot)

        assertEquals(originalSnapshot.toGson(), deserializedSnapshot.toGson())
    }
}

class KotlinClassesClasspathSnapshotSerializerTest : ClasspathSnapshotSerializerTest() {
    override val testSourceFile = SimpleKotlinClass(tmpDir)
}
