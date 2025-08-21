/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("OPT_IN_USAGE")

package kotlin

import kotlin.wasm.internal.JsBuiltin
import kotlin.wasm.internal.JsStringRef
import kotlin.wasm.internal.WasmCharArray


@kotlin.internal.IntrinsicConstEvaluation
@JsBuiltin(
    "js-string",
    "length",
    """export function length(s) {
    return s.length;
}
"""
)
internal external fun jsLength(a: JsString): Int


@kotlin.internal.IntrinsicConstEvaluation
@JsBuiltin(
    "js-string",
    "concat",
    """export function concat(a, b) {
    return a + b;
}
"""
)
internal external fun jsConcat(a: JsString, b: JsString): JsStringRef


@JsBuiltin(
    "js-string",
    "charCodeAt",
    """export function charCodeAt(s, i) {
    return s.charCodeAt(i >>> 0);
}
"""
)
internal external fun jsCharCodeAt(s: JsString, i: Int): Int


@JsBuiltin(
    "js-string",
    "substring",
    """export function substring(s, start, end) {
    start >>>= 0;
    end >>>= 0;
    return s.substring(start, end);
}
"""
)
internal external fun jsSubstring(s: JsString, start: Int, end: Int): JsStringRef


@JsBuiltin(
    "js-string",
    "compare",
    """export function compare(a, b) {
    return a === b ? 0 : a < b ? -1 : 1;
}
"""
)
internal external fun jsCompare(a: JsAny, b: JsAny): Int


@JsBuiltin(
    "js-string",
    "equals",
    """export function equals(a, b) {
    return a === b ? 1 : 0;
}
"""
)
internal external fun jsEquals(a: JsAny, b: JsAny): Int


@Suppress("WRONG_JS_INTEROP_TYPE")
@JsBuiltin(
    "js-string",
    "intoCharCodeArray",
    """const moduleIntoCharCode = new WebAssembly.Module(new Uint8Array([
  0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, 0x01, 0x0b, 0x02, 0x5e,
  0x77, 0x01, 0x60, 0x03, 0x64, 0x00, 0x7f, 0x7f, 0x00, 0x03, 0x02, 0x01,
  0x01, 0x07, 0x0b, 0x01, 0x07, 0x61, 0x31, 0x36, 0x5f, 0x73, 0x65, 0x74,
  0x00, 0x00, 0x0a, 0x0d, 0x01, 0x0b, 0x00, 0x20, 0x00, 0x20, 0x01, 0x20,
  0x02, 0xfb, 0x0e, 0x00, 0x0b, 0x00, 0x13, 0x04, 0x6e, 0x61, 0x6d, 0x65,
  0x04, 0x0c, 0x01, 0x00, 0x09, 0x61, 0x72, 0x72, 0x61, 0x79, 0x5f, 0x69,
  0x31, 0x36
]));
const helpersIntoCharCode = new WebAssembly.Instance(moduleIntoCharCode).exports;

export function intoCharCodeArray(s, array, start) {
    start >>>= 0;
    for (let i = 0; i < s.length; i++) {
      helpersIntoCharCode.a16_set(array, start + i, s.charCodeAt(i));
    }
    return s.length;
}
"""
)
internal external fun jsIntoCharCodeArray(string: JsAny, array: WasmCharArray, start: Int): Int


@Suppress("WRONG_JS_INTEROP_TYPE")
@JsBuiltin(
    "js-string",
    "fromCharCodeArray",
    """const moduleFromCharCodeArray = new WebAssembly.Module(new Uint8Array([
    0,   97,  115, 109, 1,   0,   0,   0,   1,   21,  4,   94,  119, 1,   96,
    1,   111, 1,   127, 96,  2,   111, 127, 1,   127, 96,  3,   111, 127, 127,
    0,   3,   4,   3,   1,   2,   3,   7,   33,  3,   9,   97,  114, 114, 97,
    121, 95,  108, 101, 110, 0,   0,   7,   97,  49,  54,  95,  103, 101, 116,
    0,   1,   7,   97,  49,  54,  95,  115, 101, 116, 0,   2,   10,  45,  3,
    11,  0,   32,  0,   251, 26,  251, 22,  106, 251, 15,  11,  14,  0,   32,
    0,   251, 26,  251, 22,  0,   32,  1,   251, 13,  0,   11,  16,  0,   32,
    0,   251, 26,  251, 22,  0,   32,  1,   32,  2,   251, 14,  0,   11,  0,
    37,  4,   110, 97,  109, 101, 1,   30,  3,   0,   9,   97,  114, 114, 97,
    121, 95,  108, 101, 110, 1,   7,   97,  49,  54,  95,  103, 101, 116, 2,
    7,   97,  49,  54,  95,  115, 101, 116
]));
const helpersFromCharCodeArray = new WebAssembly.Instance(moduleFromCharCodeArray).exports;

export function fromCharCodeArray(array, start, end) {
    start >>>= 0;
    end >>>= 0;
    let result = [];
    for (let i = start; i < end; i++) {
        result.push(String.fromCharCode(helpersFromCharCodeArray.a16_get(array, i)));
    }
    return result.join("");
}
"""
)
internal external fun jsFromCharCodeArray(array: WasmCharArray, start: Int, end: Int): JsStringRef
