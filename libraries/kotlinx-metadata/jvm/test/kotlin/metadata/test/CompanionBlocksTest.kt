/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.metadata.test

import kotlin.metadata.ExperimentalCompanionBlocks
import kotlin.metadata.ExperimentalCompanionExtensions
import kotlin.metadata.KmClassifier
import kotlin.metadata.isCompanionBlockMember
import kotlin.test.*

private class C {
    companion {
        fun foo(): Int = 42
        val bar: String = "bar"
    }
    companion object {
        @JvmStatic
        fun objFoo(): Int = 42
        val objBar: String = "bar"
    }
}

private companion fun C.baz(): Int = 42
private companion var C.qux: Int = 1

class CompanionBlocksTest {
    @OptIn(ExperimentalCompanionBlocks::class)
    @Test
    fun testCompanionBlockMembers() {
        val cMetadata = C::class.java.readMetadataAsKmClass()
        assertTrue(cMetadata.functions.single { it.name == "foo" }.isCompanionBlockMember)
        assertTrue(cMetadata.properties.single { it.name == "bar" }.isCompanionBlockMember)

        val cCompanionMetadata = C.Companion::class.java.readMetadataAsKmClass()
        assertFalse(cCompanionMetadata.functions.single { it.name == "objFoo" }.isCompanionBlockMember)
        assertFalse(cCompanionMetadata.properties.single { it.name == "objBar" }.isCompanionBlockMember)

        // These are extensions
        val packageMetadata = Class.forName("kotlin.metadata.test.CompanionBlocksTestKt").readMetadataAsKmPackage()
        assertFalse(packageMetadata.functions.single { it.name == "baz" }.isCompanionBlockMember)
        assertFalse(packageMetadata.properties.single { it.name == "qux" }.isCompanionBlockMember)
    }

    @OptIn(ExperimentalCompanionExtensions::class)
    @Test
    fun testCompanionExtensionReceivers() {
        val packageMetadata = Class.forName("kotlin.metadata.test.CompanionBlocksTestKt").readMetadataAsKmPackage()
        val bazReceiver = packageMetadata.functions.single { it.name == "baz" }.companionExtensionReceiverType
        assertNotNull(bazReceiver)
        assertEquals("kotlin/metadata/test/C", (bazReceiver.classifier as? KmClassifier.Class)?.name)

        val quxReceiver = packageMetadata.properties.single { it.name == "qux" }.companionExtensionReceiverType
        assertNotNull(quxReceiver)
        assertEquals("kotlin/metadata/test/C", (quxReceiver.classifier as? KmClassifier.Class)?.name)

        // No receivers for members
        val cMetadata = C::class.java.readMetadataAsKmClass()
        assertNull(cMetadata.functions.single { it.name == "foo" }.companionExtensionReceiverType)
        assertNull(cMetadata.properties.single { it.name == "bar" }.companionExtensionReceiverType)

        val cCompanionMetadata = C.Companion::class.java.readMetadataAsKmClass()
        assertNull(cCompanionMetadata.functions.single { it.name == "objFoo" }.companionExtensionReceiverType)
        assertNull(cCompanionMetadata.properties.single { it.name == "objBar" }.companionExtensionReceiverType)
    }
}
