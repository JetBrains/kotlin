/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

@PublishedApi
@JsNativeImplementation("""
[Int8Array, Int16Array, Uint16Array, Int32Array, Float32Array, Float64Array].forEach(function (TypedArray) {
    if (typeof TypedArray.prototype.sort === "undefined") {
        Object.defineProperty(TypedArray.prototype, 'sort', {
            value: function(compareFunction) {
                compareFunction = compareFunction || function (a, b) {
                    if (a < b) return -1;
                    if (a > b) return 1;
                    if (a === b) {
                        if (a !== 0) return 0;
                        var ia = 1 / a;
                        return ia === 1 / b ? 0 : (ia < 0 ? -1 : 1);
                    }
                    return a !== a ? (b !== b ? 0 : 1) : -1
                }
                return Array.prototype.sort.call(this, compareFunction || totalOrderComparator);
            }
        });
    }
})
""")
internal fun Any.nativeSort(): Unit {
    asDynamic().sort()
}
