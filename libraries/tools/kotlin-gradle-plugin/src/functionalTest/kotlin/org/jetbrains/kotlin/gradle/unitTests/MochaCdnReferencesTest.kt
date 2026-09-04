/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.targets.js.testing.replaceMochaCdnReferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MochaCdnReferencesTest {

    @Test
    fun `unversioned CDN references are replaced`() {
        assertEquals(
            MochaAssetReferences(styles = "mocha.css", script = "mocha.js"),
            replaceMochaCdnReferences(testHtml("https://unpkg.com/mocha"))?.mochaAssetReferences(),
        )
    }

    @Test
    fun `version pinned CDN references are replaced`() {
        assertEquals(
            MochaAssetReferences(styles = "mocha.css", script = "mocha.js"),
            replaceMochaCdnReferences(testHtml("https://unpkg.com/mocha@11.8.0"))?.mochaAssetReferences(),
        )
    }

    @Test
    fun `a page without CDN references is reported as not replaced`() {
        assertNull(replaceMochaCdnReferences(testHtml(".")))
    }
}

private data class MochaAssetReferences(val styles: String?, val script: String?)

private fun testHtml(mochaLocation: String) =
    """
    <!DOCTYPE html>
    <html>
    <head>
        <link rel="stylesheet" href="$mochaLocation/mocha.css" />
    </head>
    <body>
    <div id="mocha"></div>
    <script src="$mochaLocation/mocha.js"></script>
    <script src="tests.bundle.js"></script>
    </body>
    </html>
    """.trimIndent()

private fun String.mochaAssetReferences() = MochaAssetReferences(
    styles = Regex("""<link[^>]*href="([^"]*)"""").find(this)?.groupValues?.get(1),
    script = Regex("""<script src="([^"]*mocha[^"]*)"""").find(this)?.groupValues?.get(1),
)
