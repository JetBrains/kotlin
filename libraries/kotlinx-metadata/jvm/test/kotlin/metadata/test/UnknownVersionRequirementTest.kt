/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.metadata.test

import kotlin.metadata.KmClass
import kotlin.metadata.internal.ClassWriter
import kotlin.metadata.jvm.JvmMetadataVersion
import kotlin.metadata.jvm.KotlinClassMetadata
import kotlin.metadata.jvm.Metadata
import kotlin.metadata.jvm.internal.writeProtoBufData
import org.jetbrains.kotlin.metadata.jvm.serialization.JvmStringTable
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UnknownVersionRequirementTest {

    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    @kotlin.internal.RequireKotlin("1.8.0", "foobar")
    class Sample

    private val requireKotlinValue = "KmVersionRequirement(kind=LANGUAGE_VERSION, level=ERROR, version=1.8.0, errorCode=null, message=foobar)"

    private fun corrupt(original: KmClass): Pair<Array<String>, Array<String>> {
        val writer = ClassWriter(JvmStringTable())
        writer.writeClass(original)
        writer.t.addVersionRequirement(239) // invalid
        val (d1, d2) = writeProtoBufData(writer.t.build(), writer.c)
        return d1 to d2
    }

    @Test
    fun incorrectVersionRequirementFailsIfMetadataIsNew() {
        val (d1, d2) = corrupt(Sample::class.java.readMetadataAsKmClass())
        val incorrect =
            Metadata(KotlinClassMetadata.CLASS_KIND, JvmMetadataVersion.LATEST_STABLE_SUPPORTED.toIntArray(), d1, d2, extraInt = 0)
        assertFailsWith<IllegalArgumentException> {
            incorrect.readMetadataAsClass()
        }
    }

    @Test
    fun incorrectVersionRequirementHandledAsUnknown() {
        val original = Sample::class.java.readMetadataAsKmClass()
        assertEquals(1, original.versionRequirements.size)
        assertEquals(
            requireKotlinValue,
            original.versionRequirements.single().toString()
        )

        val (d1, d2) = corrupt(original)
        val incorrect =
            Metadata(KotlinClassMetadata.CLASS_KIND, intArrayOf(1, 1, 3), d1, d2, extraInt = 0)

        val withInvalidRequirement = incorrect.readMetadataAsClass()
        assertEquals(2, withInvalidRequirement.kmClass.versionRequirements.size)
        assertEquals(
            "[$requireKotlinValue, KmVersionRequirement(kind=UNKNOWN, level=HIDDEN, version=256.256.256, errorCode=null, message=null)]",
            withInvalidRequirement.kmClass.versionRequirements.toString()
        )

        withInvalidRequirement.version = JvmMetadataVersion.LATEST_STABLE_SUPPORTED
        val rewritten = withInvalidRequirement.write().readAsKmClass()
        assertEquals(1, rewritten.versionRequirements.size)
        assertEquals(
            requireKotlinValue,
            rewritten.versionRequirements.single().toString()
        )
    }
}
