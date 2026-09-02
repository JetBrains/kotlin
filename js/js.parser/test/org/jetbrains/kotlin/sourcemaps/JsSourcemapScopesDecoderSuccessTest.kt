/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sourcemaps

import org.jetbrains.kotlin.js.parser.sourcemaps.ECMA426BasedSourceMapParser.BindingRecord
import org.jetbrains.kotlin.js.parser.sourcemaps.ECMA426BasedSourceMapParser.DecodedSourceRecord
import org.jetbrains.kotlin.js.parser.sourcemaps.ECMA426BasedSourceMapParser.GeneratedRangeCallSiteRecord
import org.jetbrains.kotlin.js.parser.sourcemaps.ECMA426BasedSourceMapParser.GeneratedRangeRecord
import org.jetbrains.kotlin.js.parser.sourcemaps.ECMA426BasedSourceMapParser.OriginalScopeRecord
import org.jetbrains.kotlin.js.parser.sourcemaps.ECMA426BasedSourceMapParser.PositionRecord
import org.jetbrains.kotlin.js.parser.sourcemaps.ECMA426BasedSourceMapParser.StackFrameType
import org.junit.jupiter.api.Test

/**
 * Tests based on the test cases taken from https://github.com/hbenl/tc39-proposal-scope-mapping/ by Holger Benl (https://github.com/hbenl)
 *
 * The original combined `scopes` field format (`<OriginalScopeTreeList>,<TopLevelItemList>` in one string) has been
 * split into a per-source `scopes` string and a shared `ranges` string, per the ECMA-426 scopes/ranges split. Each
 * fixture below was re-split at that boundary, and every `RangeDefinition` occurrence was migrated from a single VLQ
 * to the two-VLQ `DefinitionSourceIdx DefinitionScopeIdx` form.
 */
class JsSourcemapScopesDecoderSuccessTest {
    /*
    Original source:
    ```javascript
    0 const n = 2;
    1
    2 function f(x, y = Math.max(x, n)) {
    3   const n = 3;
    4   console.log(y);
    5   console.log(n);
    6 }
    7
    8 f(1);
    ```

    Generated source:
    ```javascript
    0 const a = 2;
    1
    2 function b(c, d = Math.max(c, a)) {
    3   const a = 3;
    4   console.log(d);
    5   console.log(a);
    6 }
    7
    8 b(1);
    ```
    */
    @Test
    fun defaultParameterTest() {
        val encodedScopes = "BCAAA,DCC,BHCKEG,DEC,BCAYG,DJ,CEB,CAA,CCF"
        val encodedRanges = "ECAAA,GIJ,EHCKAC,GKL,ECYAC,GI,FEB,FA,FCF"

        val expectedOriginalScopes = [OriginalScopeRecord(
            start = PositionRecord(line = 0u, column = 0u),
            end = PositionRecord(line = 8u, column = 5u),
            name = null,
            kind = "module",
            variables = ["n", "f"],
            children = [OriginalScopeRecord(
                start = PositionRecord(line = 2u, column = 10u),
                end = PositionRecord(line = 6u, column = 1u),
                name = "f",
                kind = "function",
                variables = ["x", "y"],
                children = [OriginalScopeRecord(
                    start = PositionRecord(line = 2u, column = 34u),
                    end = PositionRecord(line = 6u, column = 1u),
                    name = null,
                    kind = "block",
                    variables = ["n"],
                    children = [],
                    isStackFrame = false,
                )],
                isStackFrame = true,
            )],
            isStackFrame = false,
        )]

        val expectedGeneratedRanges = { source: DecodedSourceRecord ->
            [GeneratedRangeRecord(
                start = PositionRecord(line = 0u, column = 0u),
                end = PositionRecord(line = 8u, column = 5u),
                definition = expectedOriginalScopes[0],
                stackFrameType = StackFrameType.NONE,
                bindings = [
                    [BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = "a")],
                    [BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = "b")],
                ],
                callSite = null,
                children = [GeneratedRangeRecord(
                    start = PositionRecord(line = 2u, column = 10u),
                    end = PositionRecord(line = 6u, column = 1u),
                    definition = expectedOriginalScopes[0].children[0],
                    stackFrameType = StackFrameType.ORIGINAL,
                    bindings = [
                        [BindingRecord(from = PositionRecord(line = 2u, column = 10u), binding = "c")],
                        [BindingRecord(from = PositionRecord(line = 2u, column = 10u), binding = "d")],
                    ],
                    callSite = null,
                    children = [GeneratedRangeRecord(
                        start = PositionRecord(line = 2u, column = 34u),
                        end = PositionRecord(line = 6u, column = 1u),
                        definition = expectedOriginalScopes[0].children[0].children[0],
                        stackFrameType = StackFrameType.NONE,
                        bindings = [
                            [BindingRecord(from = PositionRecord(line = 2u, column = 34u), binding = "a")],
                        ],
                        callSite = null,
                        children = [],
                    )],
                )],
            )]
        }

        val scopeNames = ["module", "n", "f", "function", "x", "y", "block", "a", "b", "c", "d"]

        testDecoder(encodedScopes, encodedRanges, expectedOriginalScopes, scopeNames, expectedGeneratedRanges)
    }

    /*
    Original source:
    ```javascript
    0 function outer(num) {
    1   function inner(value) {
    2     const value_plus_one = value + 1;
    3     console.log(value_plus_one);
    4   }
    5   const num_plus_one = num + 1;
    6   inner(num_plus_one);
    7 }
    8 outer(1);
    ```

    Generated source:
    ```javascript
    0 function f(a) {
    1   function g(a) {
    2     const b = a + 1;
    3     console.log(b);
    4   }
    5   const b = a + 1;
    6   g(b);
    7 }
    8 f(1);
    ```
    */
    @Test
    fun variableShadowingTest() {
        val encodedScopes = "BCAAA,DC,BHAACE,DECC,BHBCEA,DCC,CDD,CDB,CBJ"
        val encodedRanges = "ECAAA,GJ,EGAAC,GKLM,EHBCAC,GLM,FDD,FDB,FBF"

        val expectedOriginalScopes = [OriginalScopeRecord(
            start = PositionRecord(line = 0u, column = 0u),
            end = PositionRecord(line = 8u, column = 9u),
            name = null,
            kind = "module",
            variables = ["outer"],
            children = [OriginalScopeRecord(
                start = PositionRecord(line = 0u, column = 0u),
                end = PositionRecord(line = 7u, column = 1u),
                name = "outer",
                kind = "function",
                variables = ["inner", "num", "num_plus_one"],
                children = [OriginalScopeRecord(
                    start = PositionRecord(line = 1u, column = 2u),
                    end = PositionRecord(line = 4u, column = 3u),
                    name = "inner",
                    kind = "function",
                    variables = ["value", "value_plus_one"],
                    children = [],
                    isStackFrame = true,
                )],
                isStackFrame = true,
            )],
            isStackFrame = false,
        )]

        val expectedGeneratedRanges = { source: DecodedSourceRecord ->
            [GeneratedRangeRecord(
                start = PositionRecord(line = 0u, column = 0u),
                end = PositionRecord(line = 8u, column = 5u),
                definition = expectedOriginalScopes[0],
                stackFrameType = StackFrameType.NONE,
                bindings = [
                    [BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = "f")],
                ],
                callSite = null,
                children = [GeneratedRangeRecord(
                    start = PositionRecord(line = 0u, column = 0u),
                    end = PositionRecord(line = 7u, column = 1u),
                    definition = expectedOriginalScopes[0].children[0],
                    stackFrameType = StackFrameType.ORIGINAL,
                    bindings = [
                        [BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = "g")],
                        [BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = "a")],
                        [BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = "b")],
                    ],
                    callSite = null,
                    children = [GeneratedRangeRecord(
                        start = PositionRecord(line = 1u, column = 2u),
                        end = PositionRecord(line = 4u, column = 3u),
                        definition = expectedOriginalScopes[0].children[0].children[0],
                        stackFrameType = StackFrameType.ORIGINAL,
                        bindings = [
                            [BindingRecord(from = PositionRecord(line = 1u, column = 2u), binding = "a")],
                            [BindingRecord(from = PositionRecord(line = 1u, column = 2u), binding = "b")],
                        ],
                        callSite = null,
                        children = [],
                    )],
                )],
            )]
        }

        val scopeNames =
            ["module", "outer", "function", "inner", "num", "num_plus_one", "value", "value_plus_one", "f", "g", "a", "b"]

        testDecoder(encodedScopes, encodedRanges, expectedOriginalScopes, scopeNames, expectedGeneratedRanges)
    }

    /*
    Original source:
    ```javascript
    0 {
    1   let x = 1;
    2   console.log(x);
    3   {
    4     let x = 2;
    5     console.log(x);
    6   }
    7   console.log(x);
    8 }
    ```

    Generated source:
    ```javascript
    0 {
    1   var x1 = 1;
    2   console.log(x1);
    3   var x2 = 2;
    4   console.log(x2);
    5   console.log(x1);
    6 }
    ```
    */
    @Test
    fun removedScopesTest() {
        val encodedScopes = "BCAAA,BCAAC,DE,BCDCA,DA,CDD,CCB,CAA"
        val encodedRanges = "ECAAA,ECAAC,GE,EDDCAC,GF,FBS,FCB,FA"

        val expectedOriginalScopes = [OriginalScopeRecord(
            start = PositionRecord(line = 0u, column = 0u),
            end = PositionRecord(line = 8u, column = 1u),
            name = null,
            kind = "module",
            variables = [],
            children = [OriginalScopeRecord(
                start = PositionRecord(line = 0u, column = 0u),
                end = PositionRecord(line = 8u, column = 1u),
                name = null,
                kind = "block",
                variables = ["x"],
                children = [OriginalScopeRecord(
                    start = PositionRecord(line = 3u, column = 2u),
                    end = PositionRecord(line = 6u, column = 3u),
                    name = null,
                    kind = "block",
                    variables = ["x"],
                    children = [],
                    isStackFrame = false,
                )],
                isStackFrame = false,
            )],
            isStackFrame = false,
        )]

        val expectedGeneratedRanges = { source: DecodedSourceRecord ->
            [GeneratedRangeRecord(
                start = PositionRecord(line = 0u, column = 0u),
                end = PositionRecord(line = 6u, column = 1u),
                definition = expectedOriginalScopes[0],
                stackFrameType = StackFrameType.NONE,
                bindings = [],
                callSite = null,
                children = [GeneratedRangeRecord(
                    start = PositionRecord(line = 0u, column = 0u),
                    end = PositionRecord(line = 6u, column = 1u),
                    definition = expectedOriginalScopes[0].children[0],
                    stackFrameType = StackFrameType.NONE,
                    bindings = [
                        [BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = "x1")],
                    ],
                    callSite = null,
                    children = [GeneratedRangeRecord(
                        start = PositionRecord(line = 3u, column = 2u),
                        end = PositionRecord(line = 4u, column = 18u),
                        definition = expectedOriginalScopes[0].children[0].children[0],
                        stackFrameType = StackFrameType.NONE,
                        bindings = [
                            [BindingRecord(from = PositionRecord(line = 3u, column = 2u), binding = "x2")],
                        ],
                        callSite = null,
                        children = [],
                    )],
                )],
            )]
        }

        val scopeNames = ["module", "block", "x", "x1", "x2"]

        testDecoder(encodedScopes, encodedRanges, expectedOriginalScopes, scopeNames, expectedGeneratedRanges)
    }

    /*
    Original source:
    ```javascript
    0 function f(x) {
    1   console.log("Lorem " + x);
    2 }
    3 function g(x) {
    4   f("ipsum");
    5   console.log("dolor sit " + x);
    6 }
    7 g("amet");
    8 console.log("consectetur adipiscing elit");
    ```

    Generated source:
    ```javascript
    0 console.log("Lorem ipsum");
    1 console.log("dolor sit amet");
    2 console.log("consectetur adipiscing elit");
    ```
    */
    @Test
    fun doubleInliningTest() {
        val encodedScopes = "BCAAA,DCC,BHAACG,DE,CCB,BHBACA,DA,CDB,CCrB"
        val encodedRanges = "ECAAA,GAA,ECAAE,GG,IAHA,ECAAD,GH,IAEC,Fb,FBe,FBrB"

        val expectedOriginalScopes = [OriginalScopeRecord(
            start = PositionRecord(line = 0u, column = 0u),
            end = PositionRecord(line = 8u, column = 43u),
            name = null,
            kind = "module",
            variables = ["f", "g"],
            children = [
                OriginalScopeRecord(
                    start = PositionRecord(line = 0u, column = 0u),
                    end = PositionRecord(line = 2u, column = 1u),
                    name = "f",
                    kind = "function",
                    variables = ["x"],
                    children = [],
                    isStackFrame = true,
                ),
                OriginalScopeRecord(
                    start = PositionRecord(line = 3u, column = 0u),
                    end = PositionRecord(line = 6u, column = 1u),
                    name = "g",
                    kind = "function",
                    variables = ["x"],
                    children = [],
                    isStackFrame = true,
                ),
            ],
            isStackFrame = false,
        )]

        val expectedGeneratedRanges = { source: DecodedSourceRecord ->
            [GeneratedRangeRecord(
                start = PositionRecord(line = 0u, column = 0u),
                end = PositionRecord(line = 2u, column = 43u),
                definition = expectedOriginalScopes[0],
                stackFrameType = StackFrameType.NONE,
                bindings = [
                    [BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = null)],
                    [BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = null)],
                ],
                callSite = null,
                children = [GeneratedRangeRecord(
                    start = PositionRecord(line = 0u, column = 0u),
                    end = PositionRecord(line = 1u, column = 30u),
                    definition = expectedOriginalScopes[0].children[1],
                    stackFrameType = StackFrameType.NONE,
                    bindings = [
                        [BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = "\"amet\"")],
                    ],
                    callSite = GeneratedRangeCallSiteRecord(source = source, line = 7u, column = 0u),
                    children = [GeneratedRangeRecord(
                        start = PositionRecord(line = 0u, column = 0u),
                        end = PositionRecord(line = 0u, column = 27u),
                        definition = expectedOriginalScopes[0].children[0],
                        stackFrameType = StackFrameType.NONE,
                        bindings = [
                            [BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = "\"ipsum\"")],
                        ],
                        callSite = GeneratedRangeCallSiteRecord(source = source, line = 4u, column = 2u),
                        children = [],
                    )],
                )],
            )]
        }

        val scopeNames = ["module", "f", "g", "function", "x", "\"amet\"", "\"ipsum\""]

        testDecoder(encodedScopes, encodedRanges, expectedOriginalScopes, scopeNames, expectedGeneratedRanges)
    }

    /*
    Original sources:
    - one.js:
    ```javascript
    0 import { f } from "./two";
    1 let num = 42;
    2 f(num);
    3 console.log(num++);
    ```

    - two.js:
    ```javascript
    0 let increment = 1;
    1 export function f(x) {
    2   console.log(x + increment++);
    3 }
    ```

    Generated source:
    ```javascript
    0 let l = 1;
    1 let o = 42;
    2 var e;
    3 (e = o),
    4 console.log(e + l++),
    5 console.log(o++);
    ```
    */
    @Test
    fun inlineAcrossModulesTest() {
        // The second source's first `ScopeVariable` delta was re-derived from a fresh (per-source) index
        // accumulator instead of continuing from the first source's, per the split decoding model.
        val encodedScopesPerSource = ["BCAAA,DCC,CDT", "BCAAA,DGF,BHBVCI,DI,CCB,CAA"]
        val encodedRanges = "ECAAA,GAH,ECACA,GIA,EDCAAC,GJ,IACA,FCV,FBR,FA"

        val expectedOriginalScopesOne = [OriginalScopeRecord(
            start = PositionRecord(line = 0u, column = 0u),
            end = PositionRecord(line = 3u, column = 19u),
            name = null,
            kind = "module",
            variables = ["f", "num"],
            children = [],
            isStackFrame = false,
        )]

        val expectedOriginalScopesTwo = [OriginalScopeRecord(
            start = PositionRecord(line = 0u, column = 0u),
            end = PositionRecord(line = 3u, column = 1u),
            name = null,
            kind = "module",
            variables = ["increment", "f"],
            children = [OriginalScopeRecord(
                start = PositionRecord(line = 1u, column = 21u),
                end = PositionRecord(line = 3u, column = 1u),
                name = "f",
                kind = "function",
                variables = ["x"],
                children = [],
                isStackFrame = true,
            )],
            isStackFrame = false,
        )]

        val expectedGeneratedRanges = { sources: List<DecodedSourceRecord> ->
            [GeneratedRangeRecord(
                start = PositionRecord(line = 0u, column = 0u),
                end = PositionRecord(line = 5u, column = 17u),
                definition = expectedOriginalScopesOne[0],
                stackFrameType = StackFrameType.NONE,
                bindings = [
                    [BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = null)],
                    [BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = "o")],
                ],
                callSite = null,
                children = [GeneratedRangeRecord(
                    start = PositionRecord(line = 0u, column = 0u),
                    end = PositionRecord(line = 5u, column = 17u),
                    definition = expectedOriginalScopesTwo[0],
                    stackFrameType = StackFrameType.NONE,
                    bindings = [
                        [BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = "l")],
                        [BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = null)],
                    ],
                    callSite = null,
                    children = [GeneratedRangeRecord(
                        start = PositionRecord(line = 2u, column = 0u),
                        end = PositionRecord(line = 4u, column = 21u),
                        definition = expectedOriginalScopesTwo[0].children[0],
                        stackFrameType = StackFrameType.NONE,
                        bindings = [
                            [BindingRecord(from = PositionRecord(line = 2u, column = 0u), binding = "e")],
                        ],
                        callSite = GeneratedRangeCallSiteRecord(source = sources[0], line = 2u, column = 0u),
                        children = [],
                    )],
                )],
            )]
        }

        val scopeNames = ["module", "f", "num", "increment", "function", "x", "o", "l", "e"]

        testDecoder(
            encodedScopesPerSource,
            encodedRanges,
            [expectedOriginalScopesOne, expectedOriginalScopesTwo],
            scopeNames,
            expectedGeneratedRanges,
        )
    }

    /*
    Original sources:
    - module.js:
    ```javascript
    0 export const MODULE_CONSTANT = 'module_constant';
    1
    2 export class Logger {
    3   static log(x) {
    4     console.log(x);
    5   }
    6 }
    ```

    - inline_across_modules.js:
    ```javascript
    0  import {Logger} from './module.js';
    1
    2  function inner(x) {
    3    Logger.log(x);
    4  }
    5
    6  function outer(x) {
    7    inner(x);
    8  }
    9
    10 outer(42);
    11 outer(null);
    ```

    Generated source:
    ```javascript
    0 console.log(42);console.log(null);
    ```
    */
    @Test
    fun inlineAcrossModules2Test() {
        // The second source's kind/name/first-variable deltas were re-derived from fresh (per-source) index
        // accumulators instead of continuing from the first source's, per the split decoding model.
        val encodedScopesPerSource = [
            "BCAAA,DCC,BHDQGI,DG,CCD,CBB",
            "BCAAA,DEIC,BHCSMI,DF,CCB,BHCSCA,DA,CCB,CDM",
        ]
        val encodedRanges = "ECACA,GAAA,ECADA,GJA,ECACC,GK,IBKA,ECAAD,GK,IBHC,ECADB,GK,IBDC,FQ,FA,FA,FA," +
                "ECAAD,GJA,ECACC,GL,IBLA,ECAAD,GL,IBHC,ECADB,GL,IBDC,FS,FA,FA,FA,FA"

        val expectedOriginalScopesModule = [OriginalScopeRecord(
            start = PositionRecord(line = 0u, column = 0u),
            end = PositionRecord(line = 6u, column = 1u),
            name = null,
            kind = "module",
            variables = ["MODULE_CONSTANT", "Logger"],
            children = [OriginalScopeRecord(
                start = PositionRecord(line = 3u, column = 16u),
                end = PositionRecord(line = 5u, column = 3u),
                name = "log",
                kind = "function",
                variables = ["x"],
                children = [],
                isStackFrame = true,
            )],
            isStackFrame = false,
        )]

        val expectedOriginalScopesInlineAcrossModules = [OriginalScopeRecord(
            start = PositionRecord(line = 0u, column = 0u),
            end = PositionRecord(line = 11u, column = 12u),
            name = null,
            kind = "module",
            variables = ["Logger", "inner", "outer"],
            children = [
                OriginalScopeRecord(
                    start = PositionRecord(line = 2u, column = 18u),
                    end = PositionRecord(line = 4u, column = 1u),
                    name = "inner",
                    kind = "function",
                    variables = ["x"],
                    children = [],
                    isStackFrame = true,
                ),
                OriginalScopeRecord(
                    start = PositionRecord(line = 6u, column = 18u),
                    end = PositionRecord(line = 8u, column = 1u),
                    name = "outer",
                    kind = "function",
                    variables = ["x"],
                    children = [],
                    isStackFrame = true,
                ),
            ],
            isStackFrame = false,
        )]

        val expectedGeneratedRanges = { sources: List<DecodedSourceRecord> ->
            [GeneratedRangeRecord(
                start = PositionRecord(line = 0u, column = 0u),
                end = PositionRecord(line = 0u, column = 34u),
                definition = expectedOriginalScopesInlineAcrossModules[0],
                stackFrameType = StackFrameType.NONE,
                bindings = [
                    [BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = null)],
                    [BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = null)],
                    [BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = null)],
                ],
                callSite = null,
                children = [
                    GeneratedRangeRecord(
                        start = PositionRecord(line = 0u, column = 0u),
                        end = PositionRecord(line = 0u, column = 16u),
                        definition = expectedOriginalScopesModule[0],
                        stackFrameType = StackFrameType.NONE,
                        bindings = [
                            [BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = "\"module_constant\"")],
                            [BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = null)],
                        ],
                        callSite = null,
                        children = [GeneratedRangeRecord(
                            start = PositionRecord(line = 0u, column = 0u),
                            end = PositionRecord(line = 0u, column = 16u),
                            definition = expectedOriginalScopesInlineAcrossModules[0].children[1],
                            stackFrameType = StackFrameType.NONE,
                            bindings = [
                                [BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = "42")],
                            ],
                            callSite = GeneratedRangeCallSiteRecord(source = sources[1], line = 10u, column = 0u),
                            children = [GeneratedRangeRecord(
                                start = PositionRecord(line = 0u, column = 0u),
                                end = PositionRecord(line = 0u, column = 16u),
                                definition = expectedOriginalScopesInlineAcrossModules[0].children[0],
                                stackFrameType = StackFrameType.NONE,
                                bindings = [
                                    [BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = "42")],
                                ],
                                callSite = GeneratedRangeCallSiteRecord(source = sources[1], line = 7u, column = 2u),
                                children = [GeneratedRangeRecord(
                                    start = PositionRecord(line = 0u, column = 0u),
                                    end = PositionRecord(line = 0u, column = 16u),
                                    definition = expectedOriginalScopesModule[0].children[0],
                                    stackFrameType = StackFrameType.NONE,
                                    bindings = [
                                        [BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = "42")],
                                    ],
                                    callSite = GeneratedRangeCallSiteRecord(source = sources[1], line = 3u, column = 2u),
                                    children = [],
                                )],
                            )],
                        )],
                    ),
                    GeneratedRangeRecord(
                        start = PositionRecord(line = 0u, column = 16u),
                        end = PositionRecord(line = 0u, column = 34u),
                        definition = expectedOriginalScopesModule[0],
                        stackFrameType = StackFrameType.NONE,
                        bindings = [
                            [BindingRecord(from = PositionRecord(line = 0u, column = 16u), binding = "\"module_constant\"")],
                            [BindingRecord(from = PositionRecord(line = 0u, column = 16u), binding = null)],
                        ],
                        callSite = null,
                        children = [GeneratedRangeRecord(
                            start = PositionRecord(line = 0u, column = 16u),
                            end = PositionRecord(line = 0u, column = 34u),
                            definition = expectedOriginalScopesInlineAcrossModules[0].children[1],
                            stackFrameType = StackFrameType.NONE,
                            bindings = [
                                [BindingRecord(from = PositionRecord(line = 0u, column = 16u), binding = "null")],
                            ],
                            callSite = GeneratedRangeCallSiteRecord(source = sources[1], line = 11u, column = 0u),
                            children = [GeneratedRangeRecord(
                                start = PositionRecord(line = 0u, column = 16u),
                                end = PositionRecord(line = 0u, column = 34u),
                                definition = expectedOriginalScopesInlineAcrossModules[0].children[0],
                                stackFrameType = StackFrameType.NONE,
                                bindings = [
                                    [BindingRecord(from = PositionRecord(line = 0u, column = 16u), binding = "null")],
                                ],
                                callSite = GeneratedRangeCallSiteRecord(source = sources[1], line = 7u, column = 2u),
                                children = [GeneratedRangeRecord(
                                    start = PositionRecord(line = 0u, column = 16u),
                                    end = PositionRecord(line = 0u, column = 34u),
                                    definition = expectedOriginalScopesModule[0].children[0],
                                    stackFrameType = StackFrameType.NONE,
                                    bindings = [
                                        [BindingRecord(from = PositionRecord(line = 0u, column = 16u), binding = "null")],
                                    ],
                                    callSite = GeneratedRangeCallSiteRecord(source = sources[1], line = 3u, column = 2u),
                                    children = [],
                                )],
                            )],
                        )],
                    ),
                ],
            )]
        }

        val scopeNames = [
            "module", "MODULE_CONSTANT", "Logger", "log", "function", "x", "inner", "outer", "\"module_constant\"", "42", "null",
        ]

        testDecoder(
            encodedScopesPerSource,
            encodedRanges,
            [expectedOriginalScopesModule, expectedOriginalScopesInlineAcrossModules],
            scopeNames,
            expectedGeneratedRanges,
        )
    }

    /*
    Original source:
    ```javascript
    0  const CALL_CHANCE = 0.5;
    1
    2  function log(x) {
    3    console.log(x);
    4  }
    5
    6  function inner(x) {
    7    log(x);
    8  }
    9
    10 function outer(x) {
    11   const shouldCall = Math.random() < CALL_CHANCE;
    12   console.log('Do we log?', shouldCall);
    13   if (shouldCall) {
    14     inner(x);
    15   }
    16 }
    17
    18 outer(42);
    19 outer(null);
    ```

    Generated source:
    ```javascript
    0 function a(c){const b=.5>Math.random();console.log("Do we log?",b);b&&console.log(c)}a(42);a(null);
    ```
    */
    @Test
    fun inlineIntoFunctionTest() {
        val encodedScopes = "BCAAA,DCCCC,BHCQEK,DE,CCB,BHCSCA,DA,CCB,BHCSCA,DAC,BCDSG,CCD,CBB,CDM"
        val encodedRanges = "ECAAA,GKAAL,EGNAG,GMN,EC5BAD,GM,IAOE,ECAAD,GM,IAHC,FO,FA,FB,FO"

        val expectedOriginalScopes = [OriginalScopeRecord(
            start = PositionRecord(line = 0u, column = 0u),
            end = PositionRecord(line = 19u, column = 12u),
            name = null,
            kind = "module",
            variables = ["CALL_CHANCE", "log", "inner", "outer"],
            children = [
                OriginalScopeRecord(
                    start = PositionRecord(line = 2u, column = 16u),
                    end = PositionRecord(line = 4u, column = 1u),
                    name = "log",
                    kind = "function",
                    variables = ["x"],
                    children = [],
                    isStackFrame = true,
                ),
                OriginalScopeRecord(
                    start = PositionRecord(line = 6u, column = 18u),
                    end = PositionRecord(line = 8u, column = 1u),
                    name = "inner",
                    kind = "function",
                    variables = ["x"],
                    children = [],
                    isStackFrame = true,
                ),
                OriginalScopeRecord(
                    start = PositionRecord(line = 10u, column = 18u),
                    end = PositionRecord(line = 16u, column = 1u),
                    name = "outer",
                    kind = "function",
                    variables = ["x", "shouldCall"],
                    children = [OriginalScopeRecord(
                        start = PositionRecord(line = 13u, column = 18u),
                        end = PositionRecord(line = 15u, column = 3u),
                        name = null,
                        kind = "block",
                        variables = [],
                        children = [],
                        isStackFrame = false,
                    )],
                    isStackFrame = true,
                ),
            ],
            isStackFrame = false,
        )]

        val expectedGeneratedRanges = { source: DecodedSourceRecord ->
            [GeneratedRangeRecord(
                start = PositionRecord(line = 0u, column = 0u),
                end = PositionRecord(line = 0u, column = 99u),
                definition = expectedOriginalScopes[0],
                stackFrameType = StackFrameType.NONE,
                bindings = [
                    [BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = "0.5")],
                    [BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = null)],
                    [BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = null)],
                    [BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = "a")],
                ],
                callSite = null,
                children = [GeneratedRangeRecord(
                    start = PositionRecord(line = 0u, column = 13u),
                    end = PositionRecord(line = 0u, column = 85u),
                    definition = expectedOriginalScopes[0].children[2],
                    stackFrameType = StackFrameType.ORIGINAL,
                    bindings = [
                        [BindingRecord(from = PositionRecord(line = 0u, column = 13u), binding = "c")],
                        [BindingRecord(from = PositionRecord(line = 0u, column = 13u), binding = "b")],
                    ],
                    callSite = null,
                    children = [GeneratedRangeRecord(
                        start = PositionRecord(line = 0u, column = 70u),
                        end = PositionRecord(line = 0u, column = 84u),
                        definition = expectedOriginalScopes[0].children[1],
                        stackFrameType = StackFrameType.NONE,
                        bindings = [
                            [BindingRecord(from = PositionRecord(line = 0u, column = 70u), binding = "c")],
                        ],
                        callSite = GeneratedRangeCallSiteRecord(source = source, line = 14u, column = 4u),
                        children = [GeneratedRangeRecord(
                            start = PositionRecord(line = 0u, column = 70u),
                            end = PositionRecord(line = 0u, column = 84u),
                            definition = expectedOriginalScopes[0].children[0],
                            stackFrameType = StackFrameType.NONE,
                            bindings = [
                                [BindingRecord(from = PositionRecord(line = 0u, column = 70u), binding = "c")],
                            ],
                            callSite = GeneratedRangeCallSiteRecord(source = source, line = 7u, column = 2u),
                            children = [],
                        )],
                    )],
                )],
            )]
        }

        val scopeNames =
            ["module", "CALL_CHANCE", "log", "inner", "outer", "function", "x", "shouldCall", "block", "0.5", "a", "c", "b"]

        testDecoder(encodedScopes, encodedRanges, expectedOriginalScopes, scopeNames, expectedGeneratedRanges)
    }

    /*
    Original source:
    ```javascript
    0 const n = 2;
    1
    2 function f(x, y = Math.max(x, n)) {
    3   const n = 3;
    4   console.log(y);
    5   console.log(n);
    6 }
    7
    8 f(1);
    ```

    Generated source:
    ```javascript
    0 console.log(Math.max(1, 2));
    1 console.log(3);
    ```
    */
    @Test
    fun inlinedDefaultParameterTest() {
        val encodedScopes = "BCAAA,DCC,BHCKEG,DEC,BDAYAG,DJ,CEB,CAA,CCF"
        val encodedRanges = "ECAAA,GIA,ECAAC,GJA,IAIA,ECAAC,GK,ECMAD,GJA,FO,FBP,FA,FA"

        val expectedOriginalScopes = [OriginalScopeRecord(
            start = PositionRecord(line = 0u, column = 0u),
            end = PositionRecord(line = 8u, column = 5u),
            name = null,
            kind = "module",
            variables = ["n", "f"],
            children = [OriginalScopeRecord(
                start = PositionRecord(line = 2u, column = 10u),
                end = PositionRecord(line = 6u, column = 1u),
                name = "f",
                kind = "function",
                variables = ["x", "y"],
                children = [OriginalScopeRecord(
                    start = PositionRecord(line = 2u, column = 34u),
                    end = PositionRecord(line = 6u, column = 1u),
                    // The name index is relative to the previously decoded one, so this scope reuses the enclosing function's name
                    name = "f",
                    kind = "block",
                    variables = ["n"],
                    children = [],
                    isStackFrame = false,
                )],
                isStackFrame = true,
            )],
            isStackFrame = false,
        )]

        val expectedGeneratedRanges = { source: DecodedSourceRecord ->
            [GeneratedRangeRecord(
                start = PositionRecord(line = 0u, column = 0u),
                end = PositionRecord(line = 1u, column = 15u),
                definition = expectedOriginalScopes[0],
                stackFrameType = StackFrameType.NONE,
                bindings = [
                    [BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = "2")],
                    [BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = null)],
                ],
                callSite = null,
                children = [GeneratedRangeRecord(
                    start = PositionRecord(line = 0u, column = 0u),
                    end = PositionRecord(line = 1u, column = 15u),
                    definition = expectedOriginalScopes[0].children[0],
                    stackFrameType = StackFrameType.NONE,
                    bindings = [
                        [BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = "1")],
                        [BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = null)],
                    ],
                    callSite = GeneratedRangeCallSiteRecord(source = source, line = 8u, column = 0u),
                    children = [GeneratedRangeRecord(
                        start = PositionRecord(line = 0u, column = 0u),
                        end = PositionRecord(line = 1u, column = 15u),
                        definition = expectedOriginalScopes[0].children[0].children[0],
                        stackFrameType = StackFrameType.NONE,
                        bindings = [
                            [BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = "3")],
                        ],
                        callSite = null,
                        children = [GeneratedRangeRecord(
                            start = PositionRecord(line = 0u, column = 12u),
                            end = PositionRecord(line = 0u, column = 26u),
                            definition = expectedOriginalScopes[0].children[0],
                            stackFrameType = StackFrameType.NONE,
                            bindings = [
                                [BindingRecord(from = PositionRecord(line = 0u, column = 12u), binding = "1")],
                                [BindingRecord(from = PositionRecord(line = 0u, column = 12u), binding = null)],
                            ],
                            callSite = null,
                            children = [],
                        )],
                    )],
                )],
            )]
        }

        val scopeNames = ["module", "n", "f", "function", "x", "y", "block", "2", "1", "3"]

        testDecoder(encodedScopes, encodedRanges, expectedOriginalScopes, scopeNames, expectedGeneratedRanges)
    }

    /*
    Original source:
    ```javascript
    0 function log(msg) {
    1   console.log(msg);
    2 }
    3 let x = "foo";
    4 log(x);
    5 x = "bar";
    6 log(x);
    ```

    Generated source:
    ```javascript
    0 console.log("foo");
    1 console.log("bar");
    ```
    */
    @Test
    fun liveRangeTest() {
        val encodedScopes = "BCAAA,DCC,BHASCG,DE,CCB,CEH"
        val encodedRanges = "ECAAA,GAG,HBBAH,ECAAC,GG,IAEB,FT,EDBAAA,GH,IAGA,FT,FA"

        val expectedOriginalScopes = [OriginalScopeRecord(
            start = PositionRecord(line = 0u, column = 0u),
            end = PositionRecord(line = 6u, column = 7u),
            name = null,
            kind = "module",
            variables = ["log", "x"],
            children = [OriginalScopeRecord(
                start = PositionRecord(line = 0u, column = 18u),
                end = PositionRecord(line = 2u, column = 1u),
                name = "log",
                kind = "function",
                variables = ["msg"],
                children = [],
                isStackFrame = true,
            )],
            isStackFrame = false,
        )]

        val expectedGeneratedRanges = { source: DecodedSourceRecord ->
            [GeneratedRangeRecord(
                start = PositionRecord(line = 0u, column = 0u),
                end = PositionRecord(line = 1u, column = 19u),
                definition = expectedOriginalScopes[0],
                stackFrameType = StackFrameType.NONE,
                bindings = [
                    [BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = null)],
                    // A sub-range binding: `x` is available as "foo" until line 1, where it becomes "bar"
                    [
                        BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = "\"foo\""),
                        BindingRecord(from = PositionRecord(line = 1u, column = 0u), binding = "\"bar\""),
                    ],
                ],
                callSite = null,
                children = [
                    GeneratedRangeRecord(
                        start = PositionRecord(line = 0u, column = 0u),
                        end = PositionRecord(line = 0u, column = 19u),
                        definition = expectedOriginalScopes[0].children[0],
                        stackFrameType = StackFrameType.NONE,
                        bindings = [
                            [BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = "\"foo\"")],
                        ],
                        callSite = GeneratedRangeCallSiteRecord(source = source, line = 4u, column = 1u),
                        children = [],
                    ),
                    GeneratedRangeRecord(
                        start = PositionRecord(line = 1u, column = 0u),
                        end = PositionRecord(line = 1u, column = 19u),
                        definition = expectedOriginalScopes[0].children[0],
                        stackFrameType = StackFrameType.NONE,
                        bindings = [
                            [BindingRecord(from = PositionRecord(line = 1u, column = 0u), binding = "\"bar\"")],
                        ],
                        callSite = GeneratedRangeCallSiteRecord(source = source, line = 6u, column = 0u),
                        children = [],
                    ),
                ],
            )]
        }

        val scopeNames = ["module", "log", "x", "function", "msg", "\"foo\"", "\"bar\""]

        testDecoder(encodedScopes, encodedRanges, expectedOriginalScopes, scopeNames, expectedGeneratedRanges)
    }

    /*
    Original source:
    ```javascript
    0 function inner(msg) {
    1   console.log(msg);
    2 }
    3 function outer() {
    4   inner("moved!");
    5 }
    6 outer();
    ```

    Generated source:
    ```javascript
    0 function outer() {
    1   (() => console.log("moved!"))();
    2 }
    3 outer();
    ```
    */
    @Test
    fun moveToCallSiteTest() {
        val encodedScopes = "BCAAA,DCC,BGAAG,DE,CCB,BGBAA,CCB,CBI"
        val encodedRanges = "ECAAA,GAD,ECAAE,EHBJAD,GG,FV,FCI,FA"

        val expectedOriginalScopes = [OriginalScopeRecord(
            start = PositionRecord(line = 0u, column = 0u),
            end = PositionRecord(line = 6u, column = 8u),
            name = null,
            kind = "module",
            variables = ["inner", "outer"],
            children = [
                OriginalScopeRecord(
                    start = PositionRecord(line = 0u, column = 0u),
                    end = PositionRecord(line = 2u, column = 1u),
                    name = null,
                    kind = "function",
                    variables = ["msg"],
                    children = [],
                    isStackFrame = true,
                ),
                OriginalScopeRecord(
                    start = PositionRecord(line = 3u, column = 0u),
                    end = PositionRecord(line = 5u, column = 1u),
                    name = null,
                    kind = "function",
                    variables = [],
                    children = [],
                    isStackFrame = true,
                ),
            ],
            isStackFrame = false,
        )]

        val expectedGeneratedRanges = { _: DecodedSourceRecord ->
            [GeneratedRangeRecord(
                start = PositionRecord(line = 0u, column = 0u),
                end = PositionRecord(line = 3u, column = 8u),
                definition = expectedOriginalScopes[0],
                stackFrameType = StackFrameType.NONE,
                bindings = [
                    [BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = null)],
                    [BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = "outer")],
                ],
                callSite = null,
                children = [GeneratedRangeRecord(
                    start = PositionRecord(line = 0u, column = 0u),
                    end = PositionRecord(line = 3u, column = 8u),
                    definition = expectedOriginalScopes[0].children[1],
                    stackFrameType = StackFrameType.NONE,
                    bindings = [],
                    callSite = null,
                    children = [GeneratedRangeRecord(
                        start = PositionRecord(line = 1u, column = 9u),
                        end = PositionRecord(line = 1u, column = 30u),
                        definition = expectedOriginalScopes[0].children[0],
                        stackFrameType = StackFrameType.ORIGINAL,
                        bindings = [
                            [BindingRecord(from = PositionRecord(line = 1u, column = 9u), binding = "\"moved!\"")],
                        ],
                        callSite = null,
                        children = [],
                    )],
                )],
            )]
        }

        val scopeNames = ["module", "inner", "outer", "function", "msg", "\"moved!\""]

        testDecoder(encodedScopes, encodedRanges, expectedOriginalScopes, scopeNames, expectedGeneratedRanges)
    }

    /*
    Original source:
    ```javascript
    0 function foo() {
    1   for (const x of [1, 2, 3]) {
    2     console.log(() => x);
    3     throw new Error("Boom!");
    4   }
    5 }
    6 foo();
    ```

    Generated source:
    ```javascript
    0 var _loop_1 = function (x) {
    1     console.log(function () { return x; });
    2     throw new Error("Boom!");
    3 };
    4 for (var _i = 0, _a = [1, 2, 3]; _i < _a.length; _i++) {
    5     var x = _a[_i];
    6     _loop_1(x);
    7 }
    ```
    */
    @Test
    fun outlineAndInlineTest() {
        val encodedScopes = "BCAAA,DC,BHAPCE,BCBdC,DG,CDD,CBB,CBG"
        val encodedRanges = "ECAAA,GA,ECAAC,IAGA,EObAC,GF,FDB,EBB3B,FDB,FA,FA"

        val expectedOriginalScopes = [OriginalScopeRecord(
            start = PositionRecord(line = 0u, column = 0u),
            end = PositionRecord(line = 6u, column = 6u),
            name = null,
            kind = "module",
            variables = ["foo"],
            children = [OriginalScopeRecord(
                start = PositionRecord(line = 0u, column = 15u),
                end = PositionRecord(line = 5u, column = 1u),
                name = "foo",
                kind = "function",
                variables = [],
                children = [OriginalScopeRecord(
                    start = PositionRecord(line = 1u, column = 29u),
                    end = PositionRecord(line = 4u, column = 3u),
                    name = null,
                    kind = "for-loop",
                    variables = ["x"],
                    children = [],
                    isStackFrame = false,
                )],
                isStackFrame = true,
            )],
            isStackFrame = false,
        )]

        val expectedGeneratedRanges = { source: DecodedSourceRecord ->
            [GeneratedRangeRecord(
                start = PositionRecord(line = 0u, column = 0u),
                end = PositionRecord(line = 7u, column = 1u),
                definition = expectedOriginalScopes[0],
                stackFrameType = StackFrameType.NONE,
                bindings = [
                    [BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = null)],
                ],
                callSite = null,
                children = [GeneratedRangeRecord(
                    start = PositionRecord(line = 0u, column = 0u),
                    end = PositionRecord(line = 7u, column = 1u),
                    definition = expectedOriginalScopes[0].children[0],
                    stackFrameType = StackFrameType.NONE,
                    bindings = [],
                    callSite = GeneratedRangeCallSiteRecord(source = source, line = 6u, column = 0u),
                    children = [
                        GeneratedRangeRecord(
                            start = PositionRecord(line = 0u, column = 27u),
                            end = PositionRecord(line = 3u, column = 1u),
                            definition = expectedOriginalScopes[0].children[0].children[0],
                            stackFrameType = StackFrameType.HIDDEN,
                            bindings = [
                                [BindingRecord(from = PositionRecord(line = 0u, column = 27u), binding = "x")],
                            ],
                            callSite = null,
                            children = [],
                        ),
                        GeneratedRangeRecord(
                            start = PositionRecord(line = 4u, column = 55u),
                            end = PositionRecord(line = 7u, column = 1u),
                            definition = null,
                            stackFrameType = StackFrameType.NONE,
                            bindings = [],
                            callSite = null,
                            children = [],
                        ),
                    ],
                )],
            )]
        }

        val scopeNames = ["module", "foo", "function", "for-loop", "x"]

        testDecoder(encodedScopes, encodedRanges, expectedOriginalScopes, scopeNames, expectedGeneratedRanges)
    }

    /*
    Original source:
    ```dart
    0  class X<T> {
    1    void foo(T a,
    2             [T? b = null]) {
    3      throw 'ouch';
    4    }
    5  }
    6
    7  main() {
    8    X<Object> x = X<String>();
    9    x.foo('abc');
    10   x.foo(123);
    11   X<int>().foo(1,2);
    12 }
    ```

    Generated source:
    ```javascript
    0  class X {
    1    constructor(type) { this.T = type; }
    2    foo$body(a, b) { throwExpression('ouch'); }
    3    foo$1$unchecked(a) { return this.foo$body(a, null); }
    4    foo$2(a, b) { return this.foo$body(checkType(a, this.T), checkTypeNullable(b, this.T)); }
    5    foo$1(a) { return this.foo$1$unchecked(checkType(a, this.T)); }
    6  }
    7  function main() {
    8    const x = new X("String");
    9    x.foo$1('abc');
    10   x.foo$1(123);
    11   new X("int").foo$body(1,2);
    12 }
    13 function checkType(value, type) {
    14   if (type === 'String' && typeof value !== 'string') throw new Error(`${'$'}{value} is not a String`);
    15   if (type === 'int' && typeof value !== 'number') throw new Error(`${'$'}{value} is not an int`);
    16   return value;
    17 }
    18 function checkTypeNullable(value, type) {
    19   return value == null ? value : checkType(value, type);
    20 }
    21 function throwExpression(str) {
    22   throw new Error(str);
    23 }
    24 main();
    ```
    */
    @Test
    fun outlineAndInline2Test() {
        val encodedScopes = "BCAAA,DCC,BHBKGI,DGC,CDD,BHDHDA,DC,CFB,CAA"
        val encodedRanges = "ECAAA,GAD,EFBN,FZ,EPBKAC,GGH,FjB,EPBRAA,GGJ,FmB,EPBHAA,GGH,F0C,EPBHAA,GGJ,F6B," +
                "EHCNAC,GI,EDCEAD,GKL,IAJE,FN,EDBEAA,GML,IAKE,FL,EDBPAA,GNO,IALL,FO,FBB," +
                "EFBS,FEB,EFBa,FCB,EFBY,FCB,FBH"

        val expectedOriginalScopes = [OriginalScopeRecord(
            start = PositionRecord(line = 0u, column = 0u),
            end = PositionRecord(line = 12u, column = 1u),
            name = null,
            kind = "module",
            variables = ["X", "main"],
            children = [
                OriginalScopeRecord(
                    start = PositionRecord(line = 1u, column = 10u),
                    end = PositionRecord(line = 4u, column = 3u),
                    name = "foo",
                    kind = "function",
                    variables = ["a", "b"],
                    children = [],
                    isStackFrame = true,
                ),
                OriginalScopeRecord(
                    start = PositionRecord(line = 7u, column = 7u),
                    end = PositionRecord(line = 12u, column = 1u),
                    name = "main",
                    kind = "function",
                    variables = ["x"],
                    children = [],
                    isStackFrame = true,
                ),
            ],
            isStackFrame = false,
        )]

        val expectedGeneratedRanges = { source: DecodedSourceRecord ->
            [GeneratedRangeRecord(
                start = PositionRecord(line = 0u, column = 0u),
                end = PositionRecord(line = 24u, column = 7u),
                definition = expectedOriginalScopes[0],
                stackFrameType = StackFrameType.NONE,
                bindings = [
                    [BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = null)],
                    [BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = "main")],
                ],
                callSite = null,
                children = [
                    // constructor(type)
                    GeneratedRangeRecord(
                        start = PositionRecord(line = 1u, column = 13u),
                        end = PositionRecord(line = 1u, column = 38u),
                        definition = null,
                        stackFrameType = StackFrameType.ORIGINAL,
                        bindings = [],
                        callSite = null,
                        children = [],
                    ),
                    // foo$body(a, b)
                    GeneratedRangeRecord(
                        start = PositionRecord(line = 2u, column = 10u),
                        end = PositionRecord(line = 2u, column = 45u),
                        definition = expectedOriginalScopes[0].children[0],
                        stackFrameType = StackFrameType.HIDDEN,
                        bindings = [
                            [BindingRecord(from = PositionRecord(line = 2u, column = 10u), binding = "a")],
                            [BindingRecord(from = PositionRecord(line = 2u, column = 10u), binding = "b")],
                        ],
                        callSite = null,
                        children = [],
                    ),
                    // foo$1$unchecked(a)
                    GeneratedRangeRecord(
                        start = PositionRecord(line = 3u, column = 17u),
                        end = PositionRecord(line = 3u, column = 55u),
                        definition = expectedOriginalScopes[0].children[0],
                        stackFrameType = StackFrameType.HIDDEN,
                        bindings = [
                            [BindingRecord(from = PositionRecord(line = 3u, column = 17u), binding = "a")],
                            [BindingRecord(from = PositionRecord(line = 3u, column = 17u), binding = "null")],
                        ],
                        callSite = null,
                        children = [],
                    ),
                    // foo$2(a, b)
                    GeneratedRangeRecord(
                        start = PositionRecord(line = 4u, column = 7u),
                        end = PositionRecord(line = 4u, column = 91u),
                        definition = expectedOriginalScopes[0].children[0],
                        stackFrameType = StackFrameType.HIDDEN,
                        bindings = [
                            [BindingRecord(from = PositionRecord(line = 4u, column = 7u), binding = "a")],
                            [BindingRecord(from = PositionRecord(line = 4u, column = 7u), binding = "b")],
                        ],
                        callSite = null,
                        children = [],
                    ),
                    // foo$1(a)
                    GeneratedRangeRecord(
                        start = PositionRecord(line = 5u, column = 7u),
                        end = PositionRecord(line = 5u, column = 65u),
                        definition = expectedOriginalScopes[0].children[0],
                        stackFrameType = StackFrameType.HIDDEN,
                        bindings = [
                            [BindingRecord(from = PositionRecord(line = 5u, column = 7u), binding = "a")],
                            [BindingRecord(from = PositionRecord(line = 5u, column = 7u), binding = "null")],
                        ],
                        callSite = null,
                        children = [],
                    ),
                    // function main()
                    GeneratedRangeRecord(
                        start = PositionRecord(line = 7u, column = 13u),
                        end = PositionRecord(line = 12u, column = 1u),
                        definition = expectedOriginalScopes[0].children[1],
                        stackFrameType = StackFrameType.ORIGINAL,
                        bindings = [
                            [BindingRecord(from = PositionRecord(line = 7u, column = 13u), binding = "x")],
                        ],
                        callSite = null,
                        children = [
                            GeneratedRangeRecord(
                                start = PositionRecord(line = 9u, column = 4u),
                                end = PositionRecord(line = 9u, column = 17u),
                                definition = expectedOriginalScopes[0].children[0],
                                stackFrameType = StackFrameType.NONE,
                                bindings = [
                                    [BindingRecord(from = PositionRecord(line = 9u, column = 4u), binding = "'abc'")],
                                    [BindingRecord(from = PositionRecord(line = 9u, column = 4u), binding = "undefined")],
                                ],
                                callSite = GeneratedRangeCallSiteRecord(source = source, line = 9u, column = 4u),
                                children = [],
                            ),
                            GeneratedRangeRecord(
                                start = PositionRecord(line = 10u, column = 4u),
                                end = PositionRecord(line = 10u, column = 15u),
                                definition = expectedOriginalScopes[0].children[0],
                                stackFrameType = StackFrameType.NONE,
                                bindings = [
                                    [BindingRecord(from = PositionRecord(line = 10u, column = 4u), binding = "123")],
                                    [BindingRecord(from = PositionRecord(line = 10u, column = 4u), binding = "undefined")],
                                ],
                                callSite = GeneratedRangeCallSiteRecord(source = source, line = 10u, column = 4u),
                                children = [],
                            ),
                            GeneratedRangeRecord(
                                start = PositionRecord(line = 11u, column = 15u),
                                end = PositionRecord(line = 11u, column = 29u),
                                definition = expectedOriginalScopes[0].children[0],
                                stackFrameType = StackFrameType.NONE,
                                bindings = [
                                    [BindingRecord(from = PositionRecord(line = 11u, column = 15u), binding = "1")],
                                    [BindingRecord(from = PositionRecord(line = 11u, column = 15u), binding = "2")],
                                ],
                                callSite = GeneratedRangeCallSiteRecord(source = source, line = 11u, column = 11u),
                                children = [],
                            ),
                        ],
                    ),
                    // function checkType(value, type)
                    GeneratedRangeRecord(
                        start = PositionRecord(line = 13u, column = 18u),
                        end = PositionRecord(line = 17u, column = 1u),
                        definition = null,
                        stackFrameType = StackFrameType.ORIGINAL,
                        bindings = [],
                        callSite = null,
                        children = [],
                    ),
                    // function checkTypeNullable(value, type)
                    GeneratedRangeRecord(
                        start = PositionRecord(line = 18u, column = 26u),
                        end = PositionRecord(line = 20u, column = 1u),
                        definition = null,
                        stackFrameType = StackFrameType.ORIGINAL,
                        bindings = [],
                        callSite = null,
                        children = [],
                    ),
                    // function throwExpression(str)
                    GeneratedRangeRecord(
                        start = PositionRecord(line = 21u, column = 24u),
                        end = PositionRecord(line = 23u, column = 1u),
                        definition = null,
                        stackFrameType = StackFrameType.ORIGINAL,
                        bindings = [],
                        callSite = null,
                        children = [],
                    ),
                ],
            )]
        }

        val scopeNames = [
            "module", "X", "main", "foo", "function", "a", "b", "x", "null", "'abc'", "undefined", "123", "1", "2",
        ]

        testDecoder(encodedScopes, encodedRanges, expectedOriginalScopes, scopeNames, expectedGeneratedRanges)
    }

    /*
    Original source:
    ```javascript
    0 async function fn() {
    1   return await 42;
    2 }
    3 fn();
    ```

    The generated source is the TypeScript down-level output for async/await: the `__awaiter` helper on
    lines 1-9, the `__generator` helper on lines 10-36, `function fn()` on lines 38-47 and `fn();` on line 48.
    See https://github.com/hbenl/tc39-proposal-scope-mapping/blob/master/test/getOriginalFrames/outline-async-await.test.ts
    for the full listing.
    */
    @Test
    fun outlineAsyncAwaitTest() {
        val encodedScopes = "BCAAA,DC,BHAUCE,CCB,CBF"
        val encodedRanges = "ECAAA,GC," +
                "ENB5C,ENBa,EM/B,FT,FE,ENB/B,ENBiB,FhC,ENBhB,FlC,ENBf,F3C,FCF,FBB," +
                "ENBpC,ENBpB,FqB,ENBrE,FQ,ENBV,EMW,FY,FD,ENBW,FVF,FBB," +
                "EHCOAC,ENB3B,ENBvB,FFJ,FBF,FBB,FBF"

        val expectedOriginalScopes = [OriginalScopeRecord(
            start = PositionRecord(line = 0u, column = 0u),
            end = PositionRecord(line = 3u, column = 5u),
            name = null,
            kind = "module",
            variables = ["fn"],
            children = [OriginalScopeRecord(
                start = PositionRecord(line = 0u, column = 20u),
                end = PositionRecord(line = 2u, column = 1u),
                name = "fn",
                kind = "function",
                variables = [],
                children = [],
                isStackFrame = true,
            )],
            isStackFrame = false,
        )]

        val expectedGeneratedRanges = { _: DecodedSourceRecord ->
            [GeneratedRangeRecord(
                start = PositionRecord(line = 0u, column = 0u),
                end = PositionRecord(line = 48u, column = 5u),
                definition = expectedOriginalScopes[0],
                stackFrameType = StackFrameType.NONE,
                bindings = [
                    [BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = "fn")],
                ],
                callSite = null,
                children = [
                    // The `__awaiter` helper
                    GeneratedRangeRecord(
                        start = PositionRecord(line = 1u, column = 89u),
                        end = PositionRecord(line = 9u, column = 1u),
                        definition = null,
                        stackFrameType = StackFrameType.HIDDEN,
                        bindings = [],
                        callSite = null,
                        children = [
                            GeneratedRangeRecord(
                                start = PositionRecord(line = 2u, column = 26u),
                                end = PositionRecord(line = 2u, column = 112u),
                                definition = null,
                                stackFrameType = StackFrameType.HIDDEN,
                                bindings = [],
                                callSite = null,
                                children = [GeneratedRangeRecord(
                                    start = PositionRecord(line = 2u, column = 89u),
                                    end = PositionRecord(line = 2u, column = 108u),
                                    definition = null,
                                    stackFrameType = StackFrameType.HIDDEN,
                                    bindings = [],
                                    callSite = null,
                                    children = [],
                                )],
                            ),
                            GeneratedRangeRecord(
                                start = PositionRecord(line = 3u, column = 63u),
                                end = PositionRecord(line = 8u, column = 5u),
                                definition = null,
                                stackFrameType = StackFrameType.HIDDEN,
                                bindings = [],
                                callSite = null,
                                children = [
                                    GeneratedRangeRecord(
                                        start = PositionRecord(line = 4u, column = 34u),
                                        end = PositionRecord(line = 4u, column = 99u),
                                        definition = null,
                                        stackFrameType = StackFrameType.HIDDEN,
                                        bindings = [],
                                        callSite = null,
                                        children = [],
                                    ),
                                    GeneratedRangeRecord(
                                        start = PositionRecord(line = 5u, column = 33u),
                                        end = PositionRecord(line = 5u, column = 102u),
                                        definition = null,
                                        stackFrameType = StackFrameType.HIDDEN,
                                        bindings = [],
                                        callSite = null,
                                        children = [],
                                    ),
                                    GeneratedRangeRecord(
                                        start = PositionRecord(line = 6u, column = 31u),
                                        end = PositionRecord(line = 6u, column = 118u),
                                        definition = null,
                                        stackFrameType = StackFrameType.HIDDEN,
                                        bindings = [],
                                        callSite = null,
                                        children = [],
                                    ),
                                ],
                            ),
                        ],
                    ),
                    // The `__generator` helper
                    GeneratedRangeRecord(
                        start = PositionRecord(line = 10u, column = 73u),
                        end = PositionRecord(line = 36u, column = 1u),
                        definition = null,
                        stackFrameType = StackFrameType.HIDDEN,
                        bindings = [],
                        callSite = null,
                        children = [
                            GeneratedRangeRecord(
                                start = PositionRecord(line = 11u, column = 41u),
                                end = PositionRecord(line = 11u, column = 83u),
                                definition = null,
                                stackFrameType = StackFrameType.HIDDEN,
                                bindings = [],
                                callSite = null,
                                children = [],
                            ),
                            GeneratedRangeRecord(
                                start = PositionRecord(line = 12u, column = 139u),
                                end = PositionRecord(line = 12u, column = 155u),
                                definition = null,
                                stackFrameType = StackFrameType.HIDDEN,
                                bindings = [],
                                callSite = null,
                                children = [],
                            ),
                            GeneratedRangeRecord(
                                start = PositionRecord(line = 13u, column = 21u),
                                end = PositionRecord(line = 13u, column = 70u),
                                definition = null,
                                stackFrameType = StackFrameType.HIDDEN,
                                bindings = [],
                                callSite = null,
                                children = [GeneratedRangeRecord(
                                    start = PositionRecord(line = 13u, column = 43u),
                                    end = PositionRecord(line = 13u, column = 67u),
                                    definition = null,
                                    stackFrameType = StackFrameType.HIDDEN,
                                    bindings = [],
                                    callSite = null,
                                    children = [],
                                )],
                            ),
                            GeneratedRangeRecord(
                                start = PositionRecord(line = 14u, column = 22u),
                                end = PositionRecord(line = 35u, column = 5u),
                                definition = null,
                                stackFrameType = StackFrameType.HIDDEN,
                                bindings = [],
                                callSite = null,
                                children = [],
                            ),
                        ],
                    ),
                    // `function fn()` itself
                    GeneratedRangeRecord(
                        start = PositionRecord(line = 38u, column = 14u),
                        end = PositionRecord(line = 47u, column = 1u),
                        definition = expectedOriginalScopes[0].children[0],
                        stackFrameType = StackFrameType.ORIGINAL,
                        bindings = [],
                        callSite = null,
                        children = [GeneratedRangeRecord(
                            start = PositionRecord(line = 39u, column = 55u),
                            end = PositionRecord(line = 46u, column = 5u),
                            definition = null,
                            stackFrameType = StackFrameType.HIDDEN,
                            bindings = [],
                            callSite = null,
                            children = [GeneratedRangeRecord(
                                start = PositionRecord(line = 40u, column = 47u),
                                end = PositionRecord(line = 45u, column = 9u),
                                definition = null,
                                stackFrameType = StackFrameType.HIDDEN,
                                bindings = [],
                                callSite = null,
                                children = [],
                            )],
                        )],
                    ),
                ],
            )]
        }

        val scopeNames = ["module", "fn", "function"]

        testDecoder(encodedScopes, encodedRanges, expectedOriginalScopes, scopeNames, expectedGeneratedRanges)
    }

    /*
    Original source:
    ```javascript
    0 function foo() {
    1   for (const x of [1, 2, 3]) {
    2     console.log(() => x);
    3     throw new Error("Boom!");
    4   }
    5 }
    6 foo();
    ```

    Generated source:
    ```javascript
    0  "use strict";
    1
    2  function foo() {
    3      var _loop_1 = function (x) {
    4          console.log(function () { return x; });
    5          throw new Error("Boom!");
    6      };
    7      for (var _i = 0, _a = [1, 2, 3]; _i < _a.length; _i++) {
    8          var x = _a[_i];
    9          _loop_1(x);
    10     }
    11 }
    12 foo();
    ```
    */
    @Test
    fun outlineForLoopTest() {
        val encodedScopes = "BCAAA,DC,BHAPCE,BCBdC,DG,CDD,CBB,CBG"
        val encodedRanges = "ECAAA,GA,EHCPAC,EPBfAC,GF,FDF,EBB7B,FDF,FBB,FBG"

        val expectedOriginalScopes = [OriginalScopeRecord(
            start = PositionRecord(line = 0u, column = 0u),
            end = PositionRecord(line = 6u, column = 6u),
            name = null,
            kind = "module",
            variables = ["foo"],
            children = [OriginalScopeRecord(
                start = PositionRecord(line = 0u, column = 15u),
                end = PositionRecord(line = 5u, column = 1u),
                name = "foo",
                kind = "function",
                variables = [],
                children = [OriginalScopeRecord(
                    start = PositionRecord(line = 1u, column = 29u),
                    end = PositionRecord(line = 4u, column = 3u),
                    name = null,
                    kind = "for-loop",
                    variables = ["x"],
                    children = [],
                    isStackFrame = false,
                )],
                isStackFrame = true,
            )],
            isStackFrame = false,
        )]

        val expectedGeneratedRanges = { _: DecodedSourceRecord ->
            [GeneratedRangeRecord(
                start = PositionRecord(line = 0u, column = 0u),
                end = PositionRecord(line = 12u, column = 6u),
                definition = expectedOriginalScopes[0],
                stackFrameType = StackFrameType.NONE,
                bindings = [
                    [BindingRecord(from = PositionRecord(line = 0u, column = 0u), binding = null)],
                ],
                callSite = null,
                children = [GeneratedRangeRecord(
                    start = PositionRecord(line = 2u, column = 15u),
                    end = PositionRecord(line = 11u, column = 1u),
                    definition = expectedOriginalScopes[0].children[0],
                    stackFrameType = StackFrameType.ORIGINAL,
                    bindings = [],
                    callSite = null,
                    children = [
                        GeneratedRangeRecord(
                            start = PositionRecord(line = 3u, column = 31u),
                            end = PositionRecord(line = 6u, column = 5u),
                            definition = expectedOriginalScopes[0].children[0].children[0],
                            stackFrameType = StackFrameType.HIDDEN,
                            bindings = [
                                [BindingRecord(from = PositionRecord(line = 3u, column = 31u), binding = "x")],
                            ],
                            callSite = null,
                            children = [],
                        ),
                        GeneratedRangeRecord(
                            start = PositionRecord(line = 7u, column = 59u),
                            end = PositionRecord(line = 10u, column = 5u),
                            definition = null,
                            stackFrameType = StackFrameType.NONE,
                            bindings = [],
                            callSite = null,
                            children = [],
                        ),
                    ],
                )],
            )]
        }

        val scopeNames = ["module", "foo", "function", "for-loop", "x"]

        testDecoder(encodedScopes, encodedRanges, expectedOriginalScopes, scopeNames, expectedGeneratedRanges)
    }

    /**
     * Three generated ranges in the same source whose `RangeDefinition`s stay within that source
     * (`sourceOffset = 0` each time), confirming the scope-index accumulator behaves as a normal signed,
     * relative accumulator when the source doesn't change: 0 -> +1 -> -1.
     */
    @Test
    fun sameSourceRangeDefinitionSequenceTest() {
        val encodedScopes = "BAAA,BAAB,CAB,CAB"
        val encodedRanges = "ECAAA,FF,ECBAC,FF,ECBAD,FF"

        val expectedOriginalScopes = [OriginalScopeRecord(
            start = PositionRecord(line = 0u, column = 0u),
            end = PositionRecord(line = 0u, column = 3u),
            name = null,
            kind = null,
            variables = [],
            children = [OriginalScopeRecord(
                start = PositionRecord(line = 0u, column = 1u),
                end = PositionRecord(line = 0u, column = 2u),
                name = null,
                kind = null,
                variables = [],
                children = [],
                isStackFrame = false,
            )],
            isStackFrame = false,
        )]

        val expectedGeneratedRanges = { _: DecodedSourceRecord ->
            [
                GeneratedRangeRecord(
                    start = PositionRecord(line = 0u, column = 0u),
                    end = PositionRecord(line = 0u, column = 5u),
                    definition = expectedOriginalScopes[0],
                    stackFrameType = StackFrameType.NONE,
                    bindings = [],
                    callSite = null,
                    children = [],
                ),
                GeneratedRangeRecord(
                    start = PositionRecord(line = 0u, column = 6u),
                    end = PositionRecord(line = 0u, column = 11u),
                    definition = expectedOriginalScopes[0].children[0],
                    stackFrameType = StackFrameType.NONE,
                    bindings = [],
                    callSite = null,
                    children = [],
                ),
                GeneratedRangeRecord(
                    start = PositionRecord(line = 0u, column = 12u),
                    end = PositionRecord(line = 0u, column = 17u),
                    definition = expectedOriginalScopes[0],
                    stackFrameType = StackFrameType.NONE,
                    bindings = [],
                    callSite = null,
                    children = [],
                ),
            ]
        }

        val scopeNames: List<String> = []

        testDecoder(encodedScopes, encodedRanges, expectedOriginalScopes, scopeNames, expectedGeneratedRanges)
    }

    /**
     * A `RangeDefinition` that jumps to a different source (nonzero signed `DefinitionSourceIdx`) uses an
     * *unsigned* `DefinitionScopeIdx` and resets the scope-index accumulator to 0 first — confirmed here by
     * leaving the first source's scope accumulator at index 1 before crossing over; without the reset, the
     * second range's zero scope offset would resolve to the (out-of-bounds) index 1 in the new source.
     */
    @Test
    fun crossSourceRangeDefinitionTest() {
        val encodedScopesPerSource = ["BAAA,BAAB,CAB,CAB", "BAAA,CAB"]
        val encodedRanges = "ECAAC,FF,ECBCA,FF"

        val expectedOriginalScopesOne = [OriginalScopeRecord(
            start = PositionRecord(line = 0u, column = 0u),
            end = PositionRecord(line = 0u, column = 3u),
            name = null,
            kind = null,
            variables = [],
            children = [OriginalScopeRecord(
                start = PositionRecord(line = 0u, column = 1u),
                end = PositionRecord(line = 0u, column = 2u),
                name = null,
                kind = null,
                variables = [],
                children = [],
                isStackFrame = false,
            )],
            isStackFrame = false,
        )]

        val expectedOriginalScopesTwo = [OriginalScopeRecord(
            start = PositionRecord(line = 0u, column = 0u),
            end = PositionRecord(line = 0u, column = 1u),
            name = null,
            kind = null,
            variables = [],
            children = [],
            isStackFrame = false,
        )]

        val expectedGeneratedRanges = { _: List<DecodedSourceRecord> ->
            [
                GeneratedRangeRecord(
                    start = PositionRecord(line = 0u, column = 0u),
                    end = PositionRecord(line = 0u, column = 5u),
                    definition = expectedOriginalScopesOne[0].children[0],
                    stackFrameType = StackFrameType.NONE,
                    bindings = [],
                    callSite = null,
                    children = [],
                ),
                GeneratedRangeRecord(
                    start = PositionRecord(line = 0u, column = 6u),
                    end = PositionRecord(line = 0u, column = 11u),
                    definition = expectedOriginalScopesTwo[0],
                    stackFrameType = StackFrameType.NONE,
                    bindings = [],
                    callSite = null,
                    children = [],
                ),
            ]
        }

        val scopeNames: List<String> = []

        testDecoder(
            encodedScopesPerSource,
            encodedRanges,
            [expectedOriginalScopesOne, expectedOriginalScopesTwo],
            scopeNames,
            expectedGeneratedRanges,
        )
    }
}
