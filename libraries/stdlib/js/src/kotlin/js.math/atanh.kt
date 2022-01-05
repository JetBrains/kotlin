/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JsQualifier("Math")
package kotlin.js

@PublishedApi
@JsName("atanh")
@JsNativeImplementation("""
if (typeof Math.atanh === "undefined") {
    var epsilon = 2.220446049250313E-16;
    var taylor_2_bound = Math.sqrt(epsilon);
    var taylor_n_bound = Math.sqrt(taylor_2_bound);
    
    Math.atanh = function(x) {
        if (Math.abs(x) < taylor_n_bound) {
            var result = x;
            if (Math.abs(x) > taylor_2_bound) {
                result += (x * x * x) / 3;
            }
            return result;
        }
        return Math.log((1 + x) / (1 - x)) / 2;
    };
}
""")
internal external fun nativeAtanh(value: Double): Double