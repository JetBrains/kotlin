/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package kotlin.metadata.test

import org.junit.Test
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.jvm.internal.getDebugMetadataAsJson
import kotlin.test.assertTrue
import com.google.gson.Gson
import kotlin.metadata.jvm.KotlinClassMetadata
import kotlin.test.assertEquals

class JsonSerializationTest {
    @Test
    fun testVariousClasses() {
        val kotlinMetadataClasses = listOf(
            KotlinClassMetadata::class.java,
            KotlinClassMetadata.FileFacade::class.java,
            KotlinClassMetadata.MultiFileClassFacade::class.java,
            KotlinClassMetadata.MultiFileClassPart::class.java,
            KotlinClassMetadata.Class::class.java,
            KotlinClassMetadata.SyntheticClass::class.java,
            KotlinClassMetadata.Unknown::class.java,
        )
        val otherClasses = listOf(
            Empty::class.java,
            ClassWithProperties::class.java,
            ObjectWithProperties::class.java
        )

        for (clazz in kotlinMetadataClasses + otherClasses) {
            testMetadataEquality(clazz)
        }
    }

    private fun testMetadataEquality(clazz: Class<*>) {
        val metadataAsJson = clazz.getDebugMetadataAsJson()
        assertTrue(metadataAsJson != null)
        assertTrue(metadataAsJson.isNotEmpty())
        val metadata = Gson().fromJson(metadataAsJson, MetadataAdapter::class.java).toMetadata()
        assertEquals(metadata, clazz.getMetadata())
    }

    private class MetadataAdapter(
        val kind: Int,
        val metadataVersion: Array<Int>,
        val data1: Array<String>,
        val data2: Array<String>,
        val extraString: String,
        val packageName: String,
        val extraInt: Int,
    ) {
        @OptIn(ExperimentalEncodingApi::class)
        fun toMetadata(): Metadata {
            return Metadata(
                kind = kind,
                metadataVersion = metadataVersion.toIntArray(),
                data1 = data1.map { String(Base64.Default.decode(it)) }.toTypedArray(),
                data2 = data2,
                extraString = extraString,
                packageName = packageName,
                extraInt = extraInt
            )
        }
    }
}

@Suppress("unused")
private class ClassWithProperties {
    companion object {
        const val COMPANION_FOO = ""
    }

    val a = 1

    val b: Int
        get() = 1 + 1

    val c: String = ""
        get() = field + ""

    var d = ""

    var e: Int = 1
        get() = field + 1
        set(value) {
            field = value + 1
        }

    val f by lazy { String() }

    lateinit var g: String

    fun getH(): String = ""
}

private class Empty

@Suppress("unused")
private object ObjectWithProperties {
    const val A = 1

    val b: Int
        get() = 1 + 1

    val c: String = ""
        get() = field + ""

    var d = ""

    var e: Int = 1
        get() = field + 1
        set(value) {
            field = value + 1
        }

    val f by lazy { String() }

    lateinit var g: String

    fun getH(): String = ""
}
