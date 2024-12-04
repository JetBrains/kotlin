/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JsQualifier("Math")
package kotlin.js
// ES6 Math polyfills
// Inverse hyperbolic function implementations derived from boost special math functions,
// Copyright Eric Ford & Hubert Holin 2001.

@PublishedApi
@JsName("sign")
@JsPolyfill("""
if (typeof Math.sign === "undefined") {
    Math.sign = function(x) {
        x = +x; // convert to a number
        if (x === 0 || isNaN(x)) {
            return Number(x);
        }
        return x > 0 ? 1 : -1;
    };
}
""")
internal external fun nativeSign(value: Number): Double

@PublishedApi
@JsName("trunc")
@JsPolyfill("""
if (typeof Math.trunc === "undefined") {
    Math.trunc = function(x) {
        if (isNaN(x)) {
            return NaN;
        }
        if (x > 0) {
            return Math.floor(x);
        }
        return Math.ceil(x);
    };
}
""")
internal external fun nativeTrunc(value: Number): Double

@PublishedApi
@JsName("sinh")
@JsPolyfill("""
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

@PublishedApi
@JsName("cosh")
@JsPolyfill("""
if (typeof Math.cosh === "undefined") {
    Math.cosh = function(x) {
        var y = Math.exp(x);
        var y1 = 1 / y;
        if (!isFinite(y) || !isFinite(y1)) return Math.exp(Math.abs(x) - Math.LN2);
        return (y + y1) / 2;
    };
}
""")
internal external fun nativeCosh(value: Double): Double

@PublishedApi
@JsName("tanh")
@JsPolyfill("""
if (typeof Math.tanh === "undefined") {
    $defineTaylorNBound
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

@PublishedApi
@JsName("asinh")
@JsPolyfill("""
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

@PublishedApi
@JsName("acosh")
@JsPolyfill("""
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

@PublishedApi
@JsName("atanh")
@JsPolyfill("""
if (typeof Math.atanh === "undefined") {
    $defineTaylorNBound
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

@PublishedApi
@JsName("log1p")
@JsPolyfill("""
if (typeof Math.log1p === "undefined") {
    $defineTaylorNBound
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

@PublishedApi
@JsName("expm1")
@JsPolyfill("""
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

@PublishedApi
@JsName("hypot")
@JsPolyfill("""
if (typeof Math.hypot === "undefined") {
    Math.hypot = function() {
        var y = 0;
        var length = arguments.length;

        for (var i = 0; i < length; i++) {
            if (arguments[i] === Infinity || arguments[i] === -Infinity) {
                return Infinity;
            }
            y += arguments[i] * arguments[i];
        }
        return Math.sqrt(y);
    };
}
""")
internal external fun nativeHypot(x: Double, y: Double): Double


@PublishedApi
@JsName("log10")
@JsPolyfill("""
if (typeof Math.log10 === "undefined") {
    Math.log10 = function(x) {
        return Math.log(x) * Math.LOG10E;
    };
}
""")
internal external fun nativeLog10(value: Double): Double

@PublishedApi
@JsName("log2")
@JsPolyfill("""
if (typeof Math.log2 === "undefined") {
    Math.log2 = function(x) {
        return Math.log(x) * Math.LOG2E;
    };
}
""")
internal external fun nativeLog2(value: Double): Double

@PublishedApi
@JsName("clz32")
@JsPolyfill("""
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