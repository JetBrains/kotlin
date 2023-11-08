/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.test

import kotlinx.metadata.jvm.JvmMetadataVersion
import kotlinx.metadata.jvm.KotlinClassMetadata
import kotlinx.metadata.jvm.Metadata
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WritersContractTest {
    val classMd = WritersContractTest::class.java.getMetadata()
    val l: () -> Unit = {}
    val lambdaMd = l::class.java.getMetadata()
    val fileFacadeMd = Class.forName("kotlinx.metadata.test.testdata.FileFacade").getMetadata()
    val multiFileFacadeMd = Class.forName("kotlinx.metadata.test.testdata.MultiFileClassFacade").getMetadata()
    val multiFilePartMd = Class.forName("kotlinx.metadata.test.testdata.MultiFileClassFacade__MultiFileClassFacade1Kt").getMetadata()

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
}
