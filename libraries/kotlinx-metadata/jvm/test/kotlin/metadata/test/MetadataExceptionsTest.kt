/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.metadata.test

import kotlin.metadata.KmClass
import kotlin.metadata.jvm.JvmMetadataVersion
import kotlin.metadata.jvm.KotlinClassMetadata
import kotlin.metadata.jvm.KotlinModuleMetadata
import kotlin.metadata.jvm.Metadata
import kotlin.metadata.jvm.UnstableMetadataApi
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion as CompilerMetadataVersion
import org.jetbrains.kotlin.metadata.jvm.JvmModuleProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.serializeToByteArray
import org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException
import org.junit.jupiter.api.Test
import kotlin.test.*

class MetadataExceptionsTest {
    @Test
    fun testReadMalformedInput() {
        val malformedInput = "abcdefdkdgwaydgyuawdfg543awyudfuiawty" // random string guaranteed by dropping ball on a keyboard
        val malformedMetadata =
            Metadata(KotlinClassMetadata.CLASS_KIND, CompilerMetadataVersion.INSTANCE.toArray(), arrayOf(malformedInput))
        val e = assertFailsWith<IllegalArgumentException> {
            (KotlinClassMetadata.readStrict(malformedMetadata) as KotlinClassMetadata.Class)
        }
        assertIs<InvalidProtocolBufferException>(e.cause)
    }

    @OptIn(UnstableMetadataApi::class)
    @Test
    fun testReadModuleWithCyclicQualifiedName() {
        val name = ProtoBuf.QualifiedNameTable.QualifiedName.newBuilder()
            .setShortName(0)
            .setKind(ProtoBuf.QualifiedNameTable.QualifiedName.Kind.CLASS)
            .setParentQualifiedName(0)
            .build()
        val module = JvmModuleProtoBuf.Module.newBuilder()
            .setStringTable(ProtoBuf.StringTable.newBuilder().addString("C").build())
            .setQualifiedNameTable(ProtoBuf.QualifiedNameTable.newBuilder().addQualifiedName(name).build())
            .addAnnotation(ProtoBuf.Annotation.newBuilder().setId(0))
            .build()
            .serializeToByteArray(CompilerMetadataVersion.INSTANCE, 0)

        assertFailsWith<IllegalArgumentException> { KotlinModuleMetadata.readStrict(module) }
    }

    @Test
    fun testWriteMalformedClass() {
        val kmClass = KmClass() // kotlin.UninitializedPropertyAccessException: lateinit property name has not been initialized
        val e = assertFailsWith<IllegalArgumentException> {
            KotlinClassMetadata.Class(kmClass, JvmMetadataVersion.LATEST_STABLE_SUPPORTED, 0).write()
        }
        assertIs<UninitializedPropertyAccessException>(e.cause)
    }

    private fun doTestVersion(version: IntArray, expectedText: String) {
        val md = Metadata(metadataVersion = version)
        val iae = assertFailsWith<IllegalArgumentException> { KotlinClassMetadata.readStrict(md) }
        assertContains(iae.message.orEmpty(), expectedText)
    }

    @Test
    fun testReadObsoleteVersion() {
        doTestVersion(intArrayOf(0, 1, 0), "version 0.1.0, while minimum supported version is 1.1.0")
        doTestVersion(intArrayOf(1, 0, 0), "version 1.0.0, while minimum supported version is 1.1.0")
        doTestVersion(intArrayOf(1, 0, 255), "version 1.0.255, while minimum supported version is 1.1.0")
    }

    @Test
    fun testReadNewerVersion() {
        val versionPlus2 = CompilerMetadataVersion.INSTANCE.next().next()
        doTestVersion(
            versionPlus2.toArray(),
            "version $versionPlus2, while maximum supported version is ${CompilerMetadataVersion.INSTANCE_NEXT}"
        )
    }

    @Test
    fun testInvalidVersion() {
        doTestVersion(intArrayOf(), "instance does not have metadataVersion in it and therefore is malformed and cannot be read")
        doTestVersion(
            CompilerMetadataVersion.INVALID_VERSION.toArray(),
            "instance does not have metadataVersion in it and therefore is malformed and cannot be read"
        )
    }
}
