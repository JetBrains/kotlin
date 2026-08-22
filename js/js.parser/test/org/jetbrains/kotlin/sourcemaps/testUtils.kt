/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sourcemaps

import org.jetbrains.kotlin.js.parser.sourcemaps.ECMA426BasedSourceMapParser
import org.jetbrains.kotlin.js.parser.sourcemaps.ECMA426BasedSourceMapParser.GeneratedRangeRecord
import org.jetbrains.kotlin.js.parser.sourcemaps.ECMA426BasedSourceMapParser.OriginalScopeRecord
import org.jetbrains.kotlin.js.parser.sourcemaps.ECMA426BasedSourceMapParser.ScopeInfoRecord
import kotlin.test.assertEquals
import kotlin.test.assertIs

internal fun testDecoder(
    encodedScopes: String,
    expectedScopes: List<OriginalScopeRecord?>,
    expectedRanges: List<GeneratedRangeRecord>,
    scopeNames: List<String>
) {
    val actualDecoded = ECMA426BasedSourceMapParser.decodeScopesInfo(encodedScopes, scopeNames)
    val result = assertIs<ECMA426BasedSourceMapParser.ParsingResult.Success<ScopeInfoRecord>>(actualDecoded).value

    assertEquals(expectedScopes, result.scopes)
    assertEquals(expectedRanges, result.ranges)
}

internal fun testDecoderFailure(encodedScopes: String, scopeNames: List<String> = []) {
    val actualDecoded = ECMA426BasedSourceMapParser.decodeScopesInfo(encodedScopes, scopeNames)

    assertIs<ECMA426BasedSourceMapParser.ParsingResult.Failure<ScopeInfoRecord>>(actualDecoded)
}
