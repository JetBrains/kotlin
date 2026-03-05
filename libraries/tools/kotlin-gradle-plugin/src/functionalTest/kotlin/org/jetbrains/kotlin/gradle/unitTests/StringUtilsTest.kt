/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.utils.decamelize
import kotlin.test.Test
import kotlin.test.assertEquals

class StringUtilsTest {

    @Test
    fun `decamelize handles simple camel case`() {
        assertEquals("camel-case", "camelCase".decamelize())
    }

    @Test
    fun `decamelize keeps acronyms grouped`() {
        assertEquals("kmp-with-java-diagnostic", "KMPWithJavaDiagnostic".decamelize())
        assertEquals("pre-hmpp-flags-error", "PreHMPPFlagsError".decamelize())
        assertEquals("xc-framework-different-inner-frameworks-name", "XCFrameworkDifferentInnerFrameworksName".decamelize())
        assertEquals(
            "kmp-android-target-is-incompatible-with-the-new-agp-kmp-plugin",
            "KMPAndroidTargetIsIncompatibleWithTheNewAgpKMPPlugin".decamelize()
        )
    }

    @Test
    fun `decamelize handles acronym suffix`() {
        assertEquals("ic-fir-misconfiguration-lv", "IcFirMisconfigurationLV".decamelize())
    }

    @Test
    fun `decamelize keeps wasm target naming stable`() {
        assertEquals("wasm-js", "wasmJs".decamelize())
        assertEquals("wasm-wasi", "wasmWasi".decamelize())
    }
}
