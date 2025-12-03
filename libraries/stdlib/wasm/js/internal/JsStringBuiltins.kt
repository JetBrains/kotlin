/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("OPT_IN_USAGE")

package kotlin.wasm.internal

import kotlin.internal.IntrinsicConstEvaluation

/**
 * The following Js builtins are internal and used for the implementation of String class in Wasm target.
 * Range/bound checks in Js code are omitted, as the String class performs checks in Kotlin code, before calling builtins.
 * When calling following functions, range/bound checks must be performed.
 */
private const val jsStringModuleName = "js-string"

@IntrinsicConstEvaluation
@JsBuiltin(
    jsStringModuleName,
    "length",
    "export const length = (s) => s.length"
)
internal external fun jsLength(a: JsString): Int


@IntrinsicConstEvaluation
@JsBuiltin(
    jsStringModuleName,
    "concat",
    "export const concat = (a, b) => a + b"
)
internal external fun jsConcat(a: JsString, b: JsString): JsStringRef


@JsBuiltin(
    jsStringModuleName,
    "charCodeAt",
    "export const charCodeAt = (s, i) => s.charCodeAt(i >>> 0)"
)
internal external fun jsCharCodeAt(s: JsString, i: Int): Int


@JsBuiltin(
    jsStringModuleName,
    "substring",
    """export const substring = (s, start, end) => {
    start >>>= 0;
    end >>>= 0;
    return s.substring(start, end);
}
"""
)
internal external fun jsSubstring(s: JsString, start: Int, end: Int): JsStringRef


@JsBuiltin(
    jsStringModuleName,
    "compare",
    """export const compare = (a, b) => {
    return a === b ? 0 : a < b ? -1 : 1;
}
"""
)
internal external fun jsCompare(a: JsString, b: JsString): Int


@JsBuiltin(
    jsStringModuleName,
    "equals",
    "export const equals = (a, b) => a === b ? 1 : 0"
)
internal external fun jsEquals(a: JsString, b: JsString): Int


/*
 * Loads the following wasm helpers:
 * ```wat
 * (module
 *   (type $array_i16 (array (mut i16)))
 *   (func (export "a16_set") (param (ref $array_i16) i32 i32)
 *     local.get 0
 *     local.get 1
 *     local.get 2
 *     array.set $array_i16
 *   )
 * )
 * ```
 */
@Suppress("WRONG_JS_INTEROP_TYPE")
@IgnorableReturnValue
@JsBuiltin(
    jsStringModuleName,
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

export const intoCharCodeArray = (s, array, start) => {
    start >>>= 0;
    for (let i = 0; i < s.length; i++) {
      helpersIntoCharCode.a16_set(array, start + i, s.charCodeAt(i));
    }
    return s.length;
}
"""
)
internal external fun jsIntoCharCodeArray(string: JsString, array: WasmCharArray, start: Int): Int


/*
 * Loads the following wasm helpers:
 * ```wat
 * (module
 *   (type $array_i16 (array (mut i16)))
 *   (func (export "a16_get") (param (ref $array_i16) i32) (result i32)
 *     local.get 0
 *     local.get 1
 *     array.get_u $array_i16
 *   )
 * )
 * ```
 */
@Suppress("WRONG_JS_INTEROP_TYPE")
@JsBuiltin(
    jsStringModuleName,
    "fromCharCodeArray",
    """const moduleFromCharCodeArray = new WebAssembly.Module(new Uint8Array([
  0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, 0x01, 0x0b, 0x02, 0x5e,
  0x77, 0x01, 0x60, 0x02, 0x64, 0x00, 0x7f, 0x01, 0x7f, 0x03, 0x02, 0x01,
  0x01, 0x07, 0x0b, 0x01, 0x07, 0x61, 0x31, 0x36, 0x5f, 0x67, 0x65, 0x74,
  0x00, 0x00, 0x0a, 0x0b, 0x01, 0x09, 0x00, 0x20, 0x00, 0x20, 0x01, 0xfb,
  0x0d, 0x00, 0x0b, 0x00, 0x13, 0x04, 0x6e, 0x61, 0x6d, 0x65, 0x04, 0x0c,
  0x01, 0x00, 0x09, 0x61, 0x72, 0x72, 0x61, 0x79, 0x5f, 0x69, 0x31, 0x36
]));
const helpersFromCharCodeArray = new WebAssembly.Instance(moduleFromCharCodeArray).exports;

export const fromCharCodeArray = (array, start, end) => {
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
