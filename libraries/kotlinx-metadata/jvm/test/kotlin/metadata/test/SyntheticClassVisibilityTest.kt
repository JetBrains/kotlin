/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.metadata.test

import kotlin.metadata.*
import org.junit.jupiter.api.Test
import kotlin.metadata.jvm.JvmMetadataVersion
import kotlin.test.*

class SyntheticClassVisibilityTest {

    @JvmDefaultWithCompatibility
    private interface PrivateInterfaceWithDefault {
        fun defaultMethod(): String = TODO()
    }

    private fun readSyntheticClassVisibility(javaClass: Class<*>): Visibility {
        val syntheticClass = javaClass.readMetadataAsSyntheticClass()

        // TODO: remove this patch after LanguageVersion and bootstrap compiler are updated to 2.5
        syntheticClass.version = JvmMetadataVersion(2, 5)
        return syntheticClass.visibility!!
    }

    private fun readSyntheticClassVisibility(className: String): Visibility =
        readSyntheticClassVisibility(Class.forName(className))

    @Test
    fun testDefaultImplVisibility() {
        // TODO: maybe shall be changed to be equal to visibility of the interface class
        assertEquals(
            Visibility.PUBLIC,
            readSyntheticClassVisibility("${PrivateInterfaceWithDefault::class.java.name}\$DefaultImpls")
        )
    }
}
