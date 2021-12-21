/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JsQualifier("Math")
package kotlin.js

import kotlin.js.math.defineTaylorNBound

@PublishedApi
@JsName("expm1")
@JsNativeImplementation("""
if (typeof Math.expm1 === "undefined") {
    $defineTaylorNBound
    Math.expm1 = function(x) {
        if (Math.abs(x) < taylor_n_bound) {
            var x2 = x * x;
            var x3 = x2 * x;
            var x4 = x3 * x;
            // approximation by taylor series in x at 0 up to order 4
            return (x4 / 24 + x3 / 6 + x2 / 2 + x);
        }
        return Math.exp(x) - 1;
    };
}
""")
internal external fun nativeExpm1(value: Double): Double