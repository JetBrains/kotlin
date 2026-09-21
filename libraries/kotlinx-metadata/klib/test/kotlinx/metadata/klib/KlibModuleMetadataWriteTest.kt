/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.klib

import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import kotlin.metadata.KmClass
import kotlin.metadata.KmClassifier
import kotlin.metadata.KmFunction
import kotlin.metadata.KmPackage
import kotlin.metadata.KmType
import kotlin.metadata.internal.common.KmModuleFragment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KlibModuleMetadataWriteTest {
    @Test
    fun testEmptyModuleFragmentsAreNotWritten() {
        val module = KlibModuleMetadata(
            name = "sample",
            fragments = listOf(
                fragment("foo"),
                fragment("foo.bar"),
                fragment("foo.bar.baz") {
                    pkg = KmPackage().apply {
                        fqName = "foo.bar.baz"
                        functions += KmFunction("baz").apply {
                            returnType = KmType().apply { classifier = KmClassifier.Class("kotlin/Unit") }
                        }
                    }
                },
                fragment("foo.bar.baz"),
                fragment("foo.qux") {
                    classes += KmClass().apply { name = "foo/qux/Qux" }
                    className += "foo/qux/Qux"
                },
                fragment("foo.empty") {
                    pkg = KmPackage().apply { fqName = "foo.empty" }
                },
            ),
            metadataVersion = KlibMetadataVersion.LATEST_STABLE_SUPPORTED,
        )

        for (writeStrategy in listOf(KlibModuleFragmentWriteStrategy.DEFAULT, ChunkedKlibModuleFragmentWriteStrategy())) {
            val serialized = module.write(writeStrategy)

            assertEquals(listOf("foo.bar.baz", "foo.qux"), serialized.fragmentNames, "Write strategy: $writeStrategy")
            assertEquals(listOf(1, 1), serialized.fragments.map { it.size }, "Write strategy: $writeStrategy")

            val header = KlibMetadataProtoBuf.Header.parseFrom(serialized.header)
            assertEquals(listOf("foo.bar.baz", "foo.qux"), header.packageFragmentNameList, "Write strategy: $writeStrategy")
            assertTrue(header.emptyPackageList.isEmpty(), "Write strategy: $writeStrategy")
        }
    }

    @Test
    fun testChunkedWriteStrategyDropsEmptyModuleFragments() {
        val chunks = ChunkedKlibModuleFragmentWriteStrategy().processPackageParts(listOf(fragment("foo"), fragment("foo")))
        assertTrue(chunks.isEmpty(), "Unexpected chunks: $chunks")
    }

    private fun fragment(fqName: String, init: KmModuleFragment.() -> Unit = {}): KmModuleFragment =
        KmModuleFragment().apply {
            this.fqName = fqName
            init()
        }
}
