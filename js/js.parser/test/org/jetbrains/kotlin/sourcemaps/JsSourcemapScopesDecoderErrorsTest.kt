/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sourcemaps

import org.jetbrains.kotlin.js.parser.sourcemaps.ECMA426BasedSourceMapParser.BindingRecord
import org.jetbrains.kotlin.js.parser.sourcemaps.ECMA426BasedSourceMapParser.DecodedSourceRecord
import org.jetbrains.kotlin.js.parser.sourcemaps.ECMA426BasedSourceMapParser.GeneratedRangeRecord
import org.jetbrains.kotlin.js.parser.sourcemaps.ECMA426BasedSourceMapParser.OriginalScopeRecord
import org.jetbrains.kotlin.js.parser.sourcemaps.ECMA426BasedSourceMapParser.PositionRecord
import org.jetbrains.kotlin.js.parser.sourcemaps.ECMA426BasedSourceMapParser.StackFrameType
import org.junit.jupiter.api.Test

/**
 * Tests of the malformed and the edge-case `scopes`/`ranges` field decoding, based on the test cases taken from
 * https://github.com/ChromeDevTools/source-map-scopes-codec (`src/decode/decode.test.ts`),
 * Copyright The Chromium Authors, BSD-3-Clause.
 *
 * Cases that exercise only original-scope grammar/tags (`B`/`C`/`D`) are routed through [testDecoderScopesFailure];
 * cases that exercise only generated-range grammar/tags (`E`/`F`/`G`/`H`/`I`) are routed through
 * [testDecoderRangesFailure]. Fixtures that used to combine both halves in one string (under the pre-split
 * `scopes` field) have been split at the scopes/ranges boundary, and any `RangeDefinition` occurrence was migrated
 * from a single VLQ to the two-VLQ `DefinitionSourceIdx DefinitionScopeIdx` form.
 */
class JsSourcemapScopesDecoderErrorsTest {
    /** `C` (original scope end) with no matching `B` (original scope start). */
    @Test
    fun originalScopeEndWithoutStartTest() {
        testDecoderScopesFailure("CAA")
    }

    /** `B` (original scope start) that is never closed by a `C` (original scope end). */
    @Test
    fun unterminatedOriginalScopeTest() {
        testDecoderScopesFailure("BAAA")
    }

    /** The stream ends in the middle of a VLQ, with the continuation bit of the last digit still set. */
    @Test
    fun truncatedVlqTest() {
        testDecoderScopesFailure("BAAg")
    }

    /** `F` (generated range end) with no matching `E` (generated range start). */
    @Test
    fun generatedRangeEndWithoutStartTest() {
        testDecoderRangesFailure("F0C")
    }

    /** `E` (generated range start) that is never closed by an `F` (generated range end). */
    @Test
    fun unterminatedGeneratedRangeTest() {
        testDecoderRangesFailure("EA0C")
    }

    /**
     * `D` (original scope variables) outside of an original scope tree. Since the `Scopes` goal symbol requires the
     * whole `scopes` string to be consumed, a `D` matching nothing at the top level is a failure rather than being
     * silently ignored.
     */
    @Test
    fun freeOriginalScopeVariablesTest() {
        testDecoderScopesFailure("DAC")
    }

    /** `G` (generated range bindings) outside of a generated range tree. */
    @Test
    fun freeGeneratedRangeBindingsTest() {
        testDecoderRangesFailure("GAD")
    }

    /** `H` (generated sub-range binding) outside of a generated range tree. */
    @Test
    fun freeSubRangeBindingTest() {
        testDecoderRangesFailure("HAAAA")
    }

    /** `I` (generated range call site) outside of a generated range tree. */
    @Test
    fun freeGeneratedRangeCallSiteTest() {
        testDecoderRangesFailure("IAAA")
    }

    /** A `ScopeVariable` whose accumulated index points past the end of the `names` field. */
    @Test
    fun scopeVariableIndexOutOfBoundsTest() {
        testDecoderScopesFailure("BAAA,DAE,CBA", scopeNames = ["a"])
    }

    /** An `OriginalScopeStart` whose `ScopeNameOrKind` index points past the end of the `names` field. */
    @Test
    fun scopeNameIndexOutOfBoundsTest() {
        testDecoderScopesFailure("BBAAC,CBA", scopeNames = ["a"])
    }

    /** An `OriginalScopeStart` whose `ScopeKind` index points past the end of the `names` field. */
    @Test
    fun scopeKindIndexOutOfBoundsTest() {
        testDecoderScopesFailure("BCAAC,CBA", scopeNames = ["a"])
    }

    /**
     * A `GeneratedRangeStart` with the `0x2` flag whose definition doesn't refer to any decoded original scope.
     * The `RangeDefinition`'s source offset is `A` (same source, source index 0), and its scope offset (`B`, a
     * signed VLQ) decodes to `Int.MIN_VALUE` per `VLQSignedValue`'s zero/negative-sign special case, which
     * `AccumulateIndex` clamps to 0 — still out of bounds against a source with no scopes at all.
     */
    @Test
    fun generatedRangeDefinitionOutOfBoundsTest() {
        val sources = [DecodedSourceRecord(url = null, content = null, ignored = false, rootScopes = [])]
        testDecoderRangesFailure("ECAAB,FC", sources)
    }

    /** A `GeneratedRangeStart` with the hidden flag (`0x8`) but without the stack frame flag (`0x4`). */
    @Test
    fun hiddenWithoutStackFrameFlagTest() {
        testDecoderRangesFailure("EIA,FC")
    }

    /**
     * A source with no scope data (represented at the JSON level as a `null` array entry, decoded before parsing
     * even starts — not as an in-band sentinel) alongside a second source that does have scope data.
     */
    @Test
    fun nullOriginalScopeTest() {
        val scopeNames = ["module", "x"]

        val expectedScope = OriginalScopeRecord(
            start = PositionRecord(line = 0u, column = 0u),
            end = PositionRecord(line = 5u, column = 1u),
            name = null,
            kind = "module",
            variables = ["x"],
            children = [],
            isStackFrame = false,
        )

        testDecoder(
            [null, "BCAAA,DC,CFB"],
            null,
            [null, [expectedScope]],
            scopeNames,
        ) { emptyList() }
    }

    /**
     * An `InvalidRangeItem` with an unknown tag (`42`) is decoded and ignored, so that the spec can add new items
     * later without breaking existing decoders.
     */
    @Test
    fun unknownItemTagIsIgnoredTest() {
        testDecoderIgnoresTrailingItem("qBBCD")
    }

    /**
     * The upstream encoder writes its vendor extension items with the tag `0x63` instead of the `/` mandated by the
     * spec, which makes them plain `InvalidRangeItem`s. Such an item is decoded and ignored too.
     */
    @Test
    fun unknownItemTagOfUpstreamVendorExtensionIsIgnoredTest() {
        testDecoderIgnoresTrailingItem("jDA")
    }

    /** A spec-conforming `VendorExtensionItem` (the `/` tag) among the top-level range items is decoded and ignored. */
    @Test
    fun vendorExtensionItemIsIgnoredTest() {
        testDecoderIgnoresTrailingItem("/BAB")
    }

    /** A spec-conforming `VendorExtensionItem` (the `/` tag) nested inside an original scope tree is ignored as well. */
    @Test
    fun vendorExtensionItemInsideOriginalScopeTreeIsIgnoredTest() {
        val encodedScopes = "BAAA,/BAB,CBA"

        val expectedOriginalScopes = [OriginalScopeRecord(
            start = PositionRecord(line = 0u, column = 0u),
            end = PositionRecord(line = 1u, column = 0u),
            name = null,
            kind = null,
            variables = [],
            children = [],
            isStackFrame = false,
        )]

        val scopeNames = ["a"]

        testDecoder(encodedScopes, null, expectedOriginalScopes, scopeNames) { emptyList() }
    }

    /**
     * A `BindingExpression` whose index points past the end of the `names` field decodes to an unavailable variable
     * rather than to a failure, as prescribed by the `BindingExpression` semantics ("Optionally report an error.
     * Return null.").
     */
    @Test
    fun bindingIndexOutOfBoundsIsNullTest() {
        val encodedRanges = "EAA,GE,FC"
        val scopeNames = ["a"]

        val expectedGeneratedRanges = { source: DecodedSourceRecord ->
            [GeneratedRangeRecord(
                start = PositionRecord(line = 0u, column = 0u),
                end = PositionRecord(line = 0u, column = 2u),
                definition = null,
                stackFrameType = StackFrameType.NONE,
                bindings = [
                    [BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = null)],
                ],
                callSite = null,
                children = [],
            )]
        }

        // "" (not null) — this source has a `scopes` entry, it's just empty, unlike the JSON-`null` case
        // exercised by nullOriginalScopeTest.
        testDecoder("", encodedRanges, [], scopeNames, expectedGeneratedRanges)
    }

    /**
     * Several `H` (generated sub-range binding) items for the same variable are concatenated into a single list of
     * binding records, and the last of them makes the variable unavailable for the rest of the range.
     *
     * Generated by the reference encoder from a range whose first variable has three sub-ranges and whose second
     * variable has a single binding for the whole range.
     */
    @Test
    fun multipleSubRangeBindingsTest() {
        val encodedScopes = "BCAAA,DCC,CBA"
        val encodedRanges = "EGAAA,GEF,HAAFGAEH,FU"

        val expectedOriginalScopes = [OriginalScopeRecord(
            start = PositionRecord(line = 0u, column = 0u),
            end = PositionRecord(line = 1u, column = 0u),
            name = null,
            kind = "module",
            variables = ["a", "b"],
            children = [],
            isStackFrame = false,
        )]

        val expectedGeneratedRanges = { source: DecodedSourceRecord ->
            [GeneratedRangeRecord(
                start = PositionRecord(line = 0u, column = 0u),
                end = PositionRecord(line = 0u, column = 20u),
                definition = expectedOriginalScopes[0],
                stackFrameType = StackFrameType.ORIGINAL,
                bindings = [
                    [
                        BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = "x"),
                        BindingRecord(from = PositionRecord(line = 0u, column = 5u), binding = "y"),
                        BindingRecord(from = PositionRecord(line = 0u, column = 9u), binding = null),
                    ],
                    [BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = "z")],
                ],
                callSite = null,
                children = [],
            )]
        }

        val scopeNames = ["module", "a", "b", "x", "z", "y"]

        testDecoder(encodedScopes, encodedRanges, expectedOriginalScopes, scopeNames, expectedGeneratedRanges)
    }

    /**
     * A `ScopeVariable` whose relative index would drive the name index accumulator below zero clamps the accumulator
     * back to zero instead of failing, as prescribed by `AccumulateIndex`.
     */
    @Test
    fun negativeVariableIndexIsClampedTest() {
        val encodedScopes = "BAAA,DAD,CBA"

        val expectedOriginalScopes = [OriginalScopeRecord(
            start = PositionRecord(line = 0u, column = 0u),
            end = PositionRecord(line = 1u, column = 0u),
            name = null,
            kind = null,
            variables = ["a", "a"],
            children = [],
            isStackFrame = false,
        )]

        val scopeNames = ["a"]

        testDecoder(encodedScopes, null, expectedOriginalScopes, scopeNames) { emptyList() }
    }

    /**
     * Checks that a single module scope followed by [ignoredItem] in the `ranges` field decodes to that scope
     * alone, with the item contributing no generated range.
     */
    private fun testDecoderIgnoresTrailingItem(ignoredItem: String) {
        val encodedScopes = "BCAAA,DC,CFB"

        val expectedOriginalScopes = [OriginalScopeRecord(
            start = PositionRecord(line = 0u, column = 0u),
            end = PositionRecord(line = 5u, column = 1u),
            name = null,
            kind = "module",
            variables = ["x"],
            children = [],
            isStackFrame = false,
        )]

        val scopeNames = ["module", "x"]

        testDecoder(encodedScopes, ignoredItem, expectedOriginalScopes, scopeNames) { emptyList() }
    }
}
