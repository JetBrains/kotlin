/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JsQualifier("Math")
package kotlin.js

@PublishedApi
@JsName("tanh")
@JsNativeImplementation("""
if (typeof Math.tanh === "undefined") {
    var epsilon = 2.220446049250313E-16;
    var taylor_2_bound = Math.sqrt(epsilon);
    var taylor_n_bound = Math.sqrt(taylor_2_bound);
    
    Math.tanh = function(x){
        if (Math.abs(x) < taylor_n_bound) {
            var result = x;
            if (Math.abs(x) > taylor_2_bound) {
                result -= (x * x * x) / 3;
            }
            return result;
        }
        else {
            var a = Math.exp(+x), b = Math.exp(-x);
            return a === Infinity ? 1 : b === Infinity ? -1 : (a - b) / (a + b);
        }
    };
}
""")
internal external fun nativeTanh(value: Double): Double