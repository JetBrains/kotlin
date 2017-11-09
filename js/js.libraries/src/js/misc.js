/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

Kotlin.compareTo = function (a, b) {
    var typeA = typeof a;
    var typeB = typeof a;
    if (Kotlin.isChar(a) && typeB === "number") {
        return Kotlin.primitiveCompareTo(a.charCodeAt(0), b);
    }
    if (typeA === "number" && Kotlin.isChar(b)) {
        return Kotlin.primitiveCompareTo(a, b.charCodeAt(0));
    }
    if (typeA === "number" || typeA === "string" || typeA === "boolean") {
        return Kotlin.primitiveCompareTo(a, b);
    }
    return a.compareTo_11rb$(b);
};

Kotlin.primitiveCompareTo = function (a, b) {
    return a < b ? -1 : a > b ? 1 : 0;
};

Kotlin.charInc = function (value) {
    return Kotlin.toChar(value+1);
};

Kotlin.charDec = function (value) {
    return Kotlin.toChar(value-1);
};

Kotlin.imul = Math.imul || imul;

Kotlin.imulEmulated = imul;

function imul(a, b) {
    return ((a & 0xffff0000) * (b & 0xffff) + (a & 0xffff) * (b | 0)) | 0;
}

(function() {
    var buf = new ArrayBuffer(8);
    var bufFloat64 = new Float64Array(buf);
    var bufFloat32 = new Float32Array(buf);
    var bufInt32 = new Int32Array(buf);
    var lowIndex = 0;
    var highIndex = 1;

    bufFloat64[0] = -1; // bff00000_00000000
    if (bufInt32[lowIndex] !== 0) {
        lowIndex = 1;
        highIndex = 0;
    }

    Kotlin.doubleToBits = function(value) {
        return Kotlin.doubleToRawBits(isNaN(value) ? NaN : value);
    };

    Kotlin.doubleToRawBits = function(value) {
        bufFloat64[0] = value;
        return Kotlin.Long.fromBits(bufInt32[lowIndex], bufInt32[highIndex]);
    };

    Kotlin.doubleFromBits = function(value) {
        bufInt32[lowIndex] = value.low_;
        bufInt32[highIndex] = value.high_;
        return bufFloat64[0];
    };

    Kotlin.floatToBits = function(value) {
        return Kotlin.floatToRawBits(isNaN(value) ? NaN : value);
    };

    Kotlin.floatToRawBits = function(value) {
        bufFloat32[0] = value;
        return bufInt32[0];
    };

    Kotlin.floatFromBits = function(value) {
        bufInt32[0] = value;
        return bufFloat32[0];
    };

    // returns zero value for number with positive sign bit and non-zero value for number with negative sign bit.
    Kotlin.doubleSignBit = function(value) {
        bufFloat64[0] = value;
        return bufInt32[highIndex] & 0x80000000;
    };

    Kotlin.numberHashCode = function(obj) {
        if ((obj | 0) === obj) {
            return obj | 0;
        }
        else {
            bufFloat64[0] = obj;
            return (bufInt32[highIndex] * 31 | 0) + bufInt32[lowIndex] | 0;
        }
    }
})();

Kotlin.ensureNotNull = function(x) {
    return x != null ? x : Kotlin.throwNPE();
};
