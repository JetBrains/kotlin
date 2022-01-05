/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JsQualifier("Math")
package kotlin.js

@PublishedApi
@JsName("acosh")
@JsNativeImplementation("""
if (typeof Math.acosh === "undefined") {
    $defineUpperTaylor2Bound
    Math.acosh = function(x) {
        if (x < 1)
        {
            return NaN;
        }
        else if (x - 1 >= taylor_n_bound)
        {
            if (x > upper_taylor_2_bound)
            {
                // approximation by laurent series in 1/x at 0+ order from -1 to 0
                return Math.log(x) + Math.LN2;
            }
            else
            {
                return Math.log(x + Math.sqrt(x * x - 1));
            }
        }
        else
        {
            var y = Math.sqrt(x - 1);
            // approximation by taylor series in y at 0 up to order 2
            var result = y;
            if (y >= taylor_2_bound)
            {
                var y3 = y * y * y;
                // approximation by taylor series in y at 0 up to order 4
                result -= y3 / 12;
            }

            return Math.sqrt(2) * result;
        }
    };
}
""")
internal external fun nativeAcosh(value: Double): Double

