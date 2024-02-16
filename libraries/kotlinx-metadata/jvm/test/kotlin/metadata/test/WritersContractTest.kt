/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.metadata.test

import kotlin.metadata.jvm.JvmMetadataVersion
import kotlin.metadata.jvm.KotlinClassMetadata
import kotlin.metadata.jvm.Metadata
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WritersContractTest {
    val classMd = WritersContractTest::class.java.getMetadata()
    val l: () -> Unit = @JvmSerializableLambda {}
    val lambdaMd = l::class.java.getMetadata()
    val fileFacadeMd = Class.forName("kotlin.metadata.test.testdata.FileFacade").getMetadata()
    val multiFileFacadeMd = Class.forName("kotlin.metadata.test.testdata.MultiFileClassFacade").getMetadata()
    val multiFilePartMd = Class.forName("kotlin.metadata.test.testdata.MultiFileClassFacade__MultiFileClassFacade1Kt").getMetadata()

    val unknown = Metadata(99, metadataVersion = intArrayOf(2, 0, 0), extraString = "blabla")

    val everyType = listOf(classMd, fileFacadeMd, lambdaMd, multiFileFacadeMd, multiFilePartMd, unknown)

    @Test
    fun lenientDataCantBeWritten() = everyType.forEach { md ->
        val lenientClass = KotlinClassMetadata.readLenient(md)
        assertFailsWith<IllegalArgumentException> { lenientClass.write() }
    }

    @Test
    fun oldVersionCantBeWritten() = everyType.forEach { md ->
        val writeableClass = KotlinClassMetadata.readStrict(md)
        writeableClass.version = JvmMetadataVersion(1, 2)
        assertFailsWith<IllegalArgumentException> { writeableClass.write() }
    }

    @Test
    fun transformIsIdentical() = everyType.forEach { before ->
        val after = KotlinClassMetadata.transform(before) { }
        // note: data1 and data2 will always differ
        assertEquals(before.kind, after.kind)
        assertContentEquals(before.metadataVersion, after.metadataVersion)
        assertEquals(before.extraInt, after.extraInt)
        assertEquals(before.extraString, after.extraString)
        assertEquals(before.packageName, after.packageName)
    }

    @Test
    fun futureVersionCantBeWritten() = everyType.forEach { before ->
        val md = KotlinClassMetadata.readStrict(before)
        md.version = JvmMetadataVersion(3, 4, 5)
        assertFailsWith<IllegalArgumentException> { md.write() }
    }

    @Test
    fun nextVersionWrite() = everyType.forEach { before ->
        val md = KotlinClassMetadata.readStrict(before)
        val ver = md.version
        assertEquals(2, ver.major) // to correctly handle future case 2.x -> 3.0
        md.version = JvmMetadataVersion(ver.major, ver.minor + 1, ver.patch)
        md.write() // OK
        md.version = JvmMetadataVersion(ver.major, ver.minor + 2, ver.patch)
        assertFailsWith<IllegalArgumentException> { md.write() }
    }
}
