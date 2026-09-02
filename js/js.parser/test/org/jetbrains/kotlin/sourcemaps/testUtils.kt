/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sourcemaps

import org.jetbrains.kotlin.js.parser.sourcemaps.ECMA426BasedSourceMapParser
import org.jetbrains.kotlin.js.parser.sourcemaps.ECMA426BasedSourceMapParser.DecodedSourceRecord
import org.jetbrains.kotlin.js.parser.sourcemaps.ECMA426BasedSourceMapParser.GeneratedRangeRecord
import org.jetbrains.kotlin.js.parser.sourcemaps.ECMA426BasedSourceMapParser.OriginalScopeRecord
import org.jetbrains.kotlin.js.parser.sourcemaps.ECMA426BasedSourceMapParser.ParsingResult.Failure
import org.jetbrains.kotlin.js.parser.sourcemaps.ECMA426BasedSourceMapParser.ParsingResult.Success
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Decodes [encodedScopes] (one entry per source, `null` meaning "no scope data for that source", mirroring the
 * `scopes` JSON field) and [encodedRanges] (the `ranges` JSON field, resolved against the decoded sources), then
 * asserts both against [expectedScopesPerSource] and the result of [expectedGeneratedRanges].
 *
 * [expectedGeneratedRanges] receives the actually-constructed [DecodedSourceRecord]s so that expectations can
 * reference them by identity (`DecodedSourceRecord` is not a data class, so a `GeneratedRangeCallSiteRecord`'s
 * `source` field must be compared against the very same instance used to decode the ranges).
 */
internal fun testDecoder(
    encodedScopes: List<String?>,
    encodedRanges: String?,
    expectedScopesPerSource: List<List<OriginalScopeRecord>?>,
    scopeNames: List<String> = [],
    expectedGeneratedRanges: (sources: List<DecodedSourceRecord>) -> List<GeneratedRangeRecord>,
) {
    val sources = encodedScopes.map { encoded ->
        val scopes = encoded?.let {
            assertIs<Success<List<OriginalScopeRecord>>>(
                ECMA426BasedSourceMapParser.decodeSourceScopes(it, scopeNames)
            ).value
        }
        DecodedSourceRecord(url = null, content = null, ignored = false, rootScopes = scopes)
    }
    assertEquals(expectedScopesPerSource, sources.map { it.rootScopes })

    val ranges = assertIs<Success<List<GeneratedRangeRecord>>>(
        ECMA426BasedSourceMapParser.decodeGeneratedRanges(encodedRanges, sources, scopeNames)
    ).value
    assertEquals(expectedGeneratedRanges(sources), ranges)
}

/** Convenience overload of [testDecoder] for the common single-source case. */
internal fun testDecoder(
    encodedScopes: String?,
    encodedRanges: String?,
    expectedScopes: List<OriginalScopeRecord>,
    scopeNames: List<String> = [],
    expectedGeneratedRanges: (source: DecodedSourceRecord) -> List<GeneratedRangeRecord>,
) {
    testDecoder(
        [encodedScopes],
        encodedRanges,
        [expectedScopes],
        scopeNames,
    ) { sources -> expectedGeneratedRanges(sources[0]) }
}

/** Asserts that decoding [encodedScopes] (a single source's `scopes` entry) fails. */
internal fun testDecoderScopesFailure(encodedScopes: String, scopeNames: List<String> = []) {
    val actualDecoded = ECMA426BasedSourceMapParser.decodeSourceScopes(encodedScopes, scopeNames)
    assertIs<Failure<List<OriginalScopeRecord>>>(actualDecoded)
}

/** Asserts that decoding [encodedRanges] against [sources] fails. */
internal fun testDecoderRangesFailure(
    encodedRanges: String,
    sources: List<DecodedSourceRecord> = [],
    scopeNames: List<String> = [],
) {
    val actualDecoded = ECMA426BasedSourceMapParser.decodeGeneratedRanges(encodedRanges, sources, scopeNames)
    assertIs<Failure<List<GeneratedRangeRecord>>>(actualDecoded)
}
