/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JsQualifier("Math")
package kotlin.js

import kotlin.js.math.defineTaylorNBound

@PublishedApi
@JsName("sinh")
@JsNativeImplementation("""
if (typeof Math.sinh === "undefined") {
    $defineTaylorNBound
    Math.sinh = function(x) {
        if (Math.abs(x) < taylor_n_bound) {
            var result = x;
            if (Math.abs(x) > taylor_2_bound) {
                result += (x * x * x) / 6;
            }
            return result;
        } else {
            var y = Math.exp(x);
            var y1 = 1 / y;
            if (!isFinite(y)) return Math.exp(x - Math.LN2);
            if (!isFinite(y1)) return -Math.exp(-x - Math.LN2);
            return (y - y1) / 2;
        }
    };
}
""")
internal external fun nativeSinh(value: Double): Double

