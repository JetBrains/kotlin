/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

@PublishedApi
@Suppress("NOTHING_TO_INLINE")
@JsNativeImplementation("""
if (typeof Array.prototype.fill === "undefined") {
    // Polyfill from https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/fill#Polyfill
    Object.defineProperty(Array.prototype, 'fill', {
        value: function (value) {
            // Steps 1-2.
            if (this == null) {
                throw new TypeError('this is null or not defined');
            }

            var O = Object(this);

            // Steps 3-5.
            var len = O.length >>> 0;

            // Steps 6-7.
            var start = arguments[1];
            var relativeStart = start >> 0;

            // Step 8.
            var k = relativeStart < 0 ?
                    Math.max(len + relativeStart, 0) :
                    Math.min(relativeStart, len);

            // Steps 9-10.
            var end = arguments[2];
            var relativeEnd = end === undefined ?
                              len : end >> 0;

            // Step 11.
            var finalValue = relativeEnd < 0 ?
                             Math.max(len + relativeEnd, 0) :
                             Math.min(relativeEnd, len);

            // Step 12.
            while (k < finalValue) {
                O[k] = value;
                k++;
            }

            // Step 13.
            return O;
        }
    });
}

[Int8Array, Int16Array, Uint16Array, Int32Array, Float32Array, Float64Array].forEach(function (TypedArray) {
    if (typeof TypedArray.prototype.fill === "undefined") {
        Object.defineProperty(TypedArray.prototype, 'fill', {
            value: Array.prototype.fill
        });
    }
})
""")
internal inline fun Any.nativeFill(element: Any?, fromIndex: Int, toIndex: Int): Unit {
    asDynamic().fill(element, fromIndex, toIndex)
}
