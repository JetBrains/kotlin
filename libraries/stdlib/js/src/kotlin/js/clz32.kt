/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JsQualifier("Math")
package kotlin.js

@PublishedApi
@JsName("clz32")
@JsNativeImplementation("""
if (typeof Math.clz32 === "undefined") {
    Math.clz32 = (function(log, LN2) {
        return function(x) {
            var asUint = x >>> 0;
            if (asUint === 0) {
                return 32;
            }
            return 31 - (log(asUint) / LN2 | 0) | 0; // the "| 0" acts like math.floor
        };
    })(Math.log, Math.LN2);
}
""")
internal external fun nativeClz32(value: Int): Int