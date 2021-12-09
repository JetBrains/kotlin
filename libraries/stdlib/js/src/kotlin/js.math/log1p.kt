/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JsQualifier("Math")
package kotlin.js

@PublishedApi
@JsName("log1p")
@JsNativeImplementation("""
if (typeof Math.log1p === "undefined") {
    var epsilon = 2.220446049250313E-16;
    var taylor_2_bound = Math.sqrt(epsilon);
    var taylor_n_bound = Math.sqrt(taylor_2_bound);
    
    Math.log1p = function(x) {
        if (Math.abs(x) < taylor_n_bound) {
            var x2 = x * x;
            var x3 = x2 * x;
            var x4 = x3 * x;
            // approximation by taylor series in x at 0 up to order 4
            return (-x4 / 4 + x3 / 3 - x2 / 2 + x);
        }
        return Math.log(x + 1);
    };
}
""")
internal external fun nativeLog1p(value: Double): Double