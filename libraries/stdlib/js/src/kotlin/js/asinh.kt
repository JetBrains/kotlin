/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JsQualifier("Math")
package kotlin.js

@PublishedApi
@JsName("asinh")
@JsNativeImplementation("""
if (typeof Math.asinh === "undefined") {
    $defineUpperTaylorNBound
    var asinh = function(x) {
        if (x >= +taylor_n_bound)
        {
            if (x > upper_taylor_n_bound)
            {
                if (x > upper_taylor_2_bound)
                {
                    // approximation by laurent series in 1/x at 0+ order from -1 to 0
                    return Math.log(x) + Math.LN2;
                }
                else
                {
                    // approximation by laurent series in 1/x at 0+ order from -1 to 1
                    return Math.log(x * 2 + (1 / (x * 2)));
                }
            }
            else
            {
                return Math.log(x + Math.sqrt(x * x + 1));
            }
        }
        else if (x <= -taylor_n_bound)
        {
            return -asinh(-x);
        }
        else
        {
            // approximation by taylor series in x at 0 up to order 2
            var result = x;
            if (Math.abs(x) >= taylor_2_bound)
            {
                var x3 = x * x * x;
                // approximation by taylor series in x at 0 up to order 4
                result -= x3 / 6;
            }
            return result;
        }
    };
    Math.asinh = asinh;
}
""")
internal external fun nativeAsinh(value: Double): Double

