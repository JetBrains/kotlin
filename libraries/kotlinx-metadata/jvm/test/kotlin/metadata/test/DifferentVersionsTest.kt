/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.metadata.test

import kotlin.metadata.jvm.KotlinClassMetadata
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class DifferentVersionsTest {
    val metadata = DifferentVersionsTest::class.java.getMetadata()

    fun Metadata.changeVersion(newVersion: IntArray) = Metadata(
        kind, newVersion,
        bytecodeVersion, data1, data2, extraString, packageName, extraInt
    )

    fun Metadata.addFlag(flags: Int) = Metadata(
        kind, metadataVersion, bytecodeVersion, data1, data2,
        extraString, packageName,
        extraInt or flags
    )

    @Test
    @Ignore("Enable when KT-74254 is fixed")
    fun readsCurrentVersion() {
        assertContentEquals(MetadataVersion.INSTANCE.toArray(), metadata.metadataVersion)
        assertIs<KotlinClassMetadata.Class>(KotlinClassMetadata.readStrict(metadata))
        assertIs<KotlinClassMetadata.Class>(KotlinClassMetadata.readLenient(metadata))
    }

    @Test
    fun readsNextVersion() {
        val md = metadata.changeVersion(MetadataVersion.INSTANCE_NEXT.toArray())
        assertIs<KotlinClassMetadata.Class>(KotlinClassMetadata.readStrict(md))
        assertIs<KotlinClassMetadata.Class>(KotlinClassMetadata.readLenient(md))
    }

    @Test
    fun readsN2Version() {
        val versionPlus2 = MetadataVersion.INSTANCE.next().next()
        val md = metadata.changeVersion(versionPlus2.toArray())
        assertFailsWith<IllegalArgumentException> { KotlinClassMetadata.readStrict(md) }
        assertIs<KotlinClassMetadata.Class>(KotlinClassMetadata.readLenient(md))
    }

    @Test
    fun readsArbitraryFutureVersion() {
        val md = metadata.changeVersion(intArrayOf(7, 5, 0))
        assertFailsWith<IllegalArgumentException> { KotlinClassMetadata.readStrict(md) }
        assertIs<KotlinClassMetadata.Class>(KotlinClassMetadata.readLenient(md))
    }

    @Test
    fun lenientCantReadPre1Version() { // strict is tested in MetadataExceptionsTest.testReadObsoleteVersion
        val md = metadata.changeVersion(intArrayOf(1, 0, 0))
        assertFailsWith<IllegalArgumentException> { KotlinClassMetadata.readLenient(md) }
    }

    @Test
    fun readsStrictSemanticsFlag() {
        val md = metadata.changeVersion(MetadataVersion.INSTANCE_NEXT.toArray()).addFlag(1 shl 3)
        assertFailsWith<IllegalArgumentException> { KotlinClassMetadata.readStrict(md) }
        assertIs<KotlinClassMetadata.Class>(KotlinClassMetadata.readLenient(md))
    }

    @Test // We decided to allow reading pre-release flag by both versions
    fun readsPreReleaseFlag() {
        val md = metadata.changeVersion(MetadataVersion.INSTANCE_NEXT.toArray()).addFlag(1 shl 1)
        assertIs<KotlinClassMetadata.Class>(KotlinClassMetadata.readStrict(md))
        assertIs<KotlinClassMetadata.Class>(KotlinClassMetadata.readLenient(md))
    }
}
