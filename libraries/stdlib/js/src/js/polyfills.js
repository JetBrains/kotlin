/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

if (typeof String.prototype.startsWith === "undefined") {
    String.prototype.startsWith = function(searchString, position) {
        position = position || 0;
        return this.lastIndexOf(searchString, position) === position;
    };
}
if (typeof String.prototype.endsWith === "undefined") {
    String.prototype.endsWith = function(searchString, position) {
        var subjectString = this.toString();
        if (position === undefined || position > subjectString.length) {
            position = subjectString.length;
        }
        position -= searchString.length;
        var lastIndex = subjectString.indexOf(searchString, position);
        return lastIndex !== -1 && lastIndex === position;
    };
}
// ES6 Math polyfills
if (typeof Math.sign === "undefined") {
    Math.sign = function(x) {
        x = +x; // convert to a number
        if (x === 0 || isNaN(x)) {
            return Number(x);
        }
        return x > 0 ? 1 : -1;
    };
}
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

(function() {
    var epsilon = 2.220446049250313E-16;
    var taylor_2_bound = Math.sqrt(epsilon);
    var taylor_n_bound = Math.sqrt(taylor_2_bound);
    var upper_taylor_2_bound = 1/taylor_2_bound;
    var upper_taylor_n_bound = 1/taylor_n_bound;

    if (typeof Math.sinh === "undefined") {
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
    if (typeof Math.cosh === "undefined") {
        Math.cosh = function(x) {
            var y = Math.exp(x);
            var y1 = 1 / y;
            if (!isFinite(y) || !isFinite(y1)) return Math.exp(Math.abs(x) - Math.LN2);
            return (y + y1) / 2;
        };
    }

    if (typeof Math.tanh === "undefined") {
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

    // Inverse hyperbolic function implementations derived from boost special math functions,
    // Copyright Eric Ford & Hubert Holin 2001.

    if (typeof Math.asinh === "undefined") {
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
    if (typeof Math.acosh === "undefined") {
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
    if (typeof Math.atanh === "undefined") {
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
    if (typeof Math.log1p === "undefined") {
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
    if (typeof Math.expm1 === "undefined") {
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
})();
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
if (typeof Math.log10 === "undefined") {
    Math.log10 = function(x) {
        return Math.log(x) * Math.LOG10E;
    };
}
if (typeof Math.log2 === "undefined") {
    Math.log2 = function(x) {
        return Math.log(x) * Math.LOG2E;
    };
}

// For HtmlUnit and PhantomJs
if (typeof ArrayBuffer.isView === "undefined") {
    ArrayBuffer.isView = function(a) {
        return a != null && a.__proto__ != null && a.__proto__.__proto__ === Int8Array.prototype.__proto__;
    };
}

(function() {
    function normalizeOffset(offset, length) {
        if (offset < 0) return Math.max(0, offset + length);
        return Math.min(offset, length);
    }
    function typedArraySlice(begin, end) {
        if (typeof end === "undefined") {
            end = this.length;
        }
        begin = normalizeOffset(begin || 0, this.length);
        end = Math.max(begin, normalizeOffset(end, this.length));
        return new this.constructor(this.subarray(begin, end));
    }

    var arrays = [Int8Array, Int16Array, Uint16Array, Int32Array, Float32Array, Float64Array];
    for (var i = 0; i < arrays.length; ++i) {
        var TypedArray = arrays[i];
        if (typeof TypedArray.prototype.slice === "undefined") {
            Object.defineProperty(TypedArray.prototype, 'slice', {
                value: typedArraySlice
            });
        }
    }

    // Patch apply to work with TypedArrays if needed.
    try {
        (function() {}).apply(null, new Int32Array(0))
    } catch (e) {
        var apply = Function.prototype.apply;
        Object.defineProperty(Function.prototype, 'apply', {
            value: function(self, array) {
                return apply.call(this, self, [].slice.call(array));
            }
        });
    }


    // Patch map to work with TypedArrays if needed.
    for (var i = 0; i < arrays.length; ++i) {
        var TypedArray = arrays[i];
        if (typeof TypedArray.prototype.map === "undefined") {
            Object.defineProperty(TypedArray.prototype, 'map', {
                value: function(callback, self) {
                    return [].slice.call(this).map(callback, self);
                }
            });
        }
    }

    // Patch sort to work with TypedArrays if needed.
    // TODO: consider to remove following function and replace it with `Kotlin.doubleCompareTo` (see misc.js)
    var totalOrderComparator = function (a, b) {
        if (a < b) return -1;
        if (a > b) return 1;

        if (a === b) {
            if (a !== 0) return 0;

            var ia = 1 / a;
            return ia === 1 / b ? 0 : (ia < 0 ? -1 : 1);
        }

        return a !== a ? (b !== b ? 0 : 1) : -1
    };

    for (var i = 0; i < arrays.length; ++i) {
        var TypedArray = arrays[i];
        if (typeof TypedArray.prototype.sort === "undefined") {
            Object.defineProperty(TypedArray.prototype, 'sort', {
                value: function(compareFunction) {
                    return Array.prototype.sort.call(this, compareFunction || totalOrderComparator);
                }
            });
        }
    }
})();
