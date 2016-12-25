/**
 * Copyright 2010 Tim Down.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Kotlin stdlib

Kotlin.equals = function (obj1, obj2) {
    if (obj1 == null) {
        return obj2 == null;
    }

    if (obj2 == null) {
        return false;
    }

    if (typeof obj1 == "object" && typeof obj1.equals === "function") {
        return obj1.equals(obj2);
    }

    return obj1 === obj2;
};

Kotlin.hashCode = function (obj) {
    if (obj == null) {
        return 0;
    }
    if ("function" == typeof obj.hashCode) {
        return obj.hashCode();
    }
    var objType = typeof obj;
    if ("object" == objType || "function" == objType) {
        return getObjectHashCode(obj);
    } else if ("number" == objType) {
        // TODO: a more elaborate code is needed for floating point values.
        return obj | 0;
    } if ("boolean" == objType) {
        return Number(obj)
    }

    var str = String(obj);
    return getStringHashCode(str);
};

Kotlin.toString = function (o) {
    if (o == null) {
        return "null";
    }
    else if (Array.isArray(o)) {
        return "[...]";
    }
    else {
        return o.toString();
    }
};

Kotlin.arrayToString = function (a) {
    return "[" + a.map(Kotlin.toString).join(", ") + "]";
};

Kotlin.arrayDeepToString = function (a, visited) {
    visited = visited || [a];
    return "[" + a.map(function(e) {
            if (Array.isArray(e) && visited.indexOf(e) < 0) {
                visited.push(e);
                var result = Kotlin.arrayDeepToString(e, visited);
                visited.pop();
                return result;
            }
            else {
                return Kotlin.toString(e);
            }
        }).join(", ") + "]";
};

Kotlin.compareTo = function (a, b) {
    var typeA = typeof a;
    var typeB = typeof a;
    if (Kotlin.isChar(a) && typeB == "number") {
        return Kotlin.primitiveCompareTo(a.charCodeAt(0), b);
    }
    if (typeA == "number" && Kotlin.isChar(b)) {
        return Kotlin.primitiveCompareTo(a, b.charCodeAt(0));
    }
    if (typeA == "number" || typeA == "string") {
        return a < b ? -1 : a > b ? 1 : 0;
    }
    return a.compareTo_za3rmp$(b);
};

Kotlin.primitiveCompareTo = function (a, b) {
    return a < b ? -1 : a > b ? 1 : 0;
};

Kotlin.isNumber = function (a) {
    return typeof a == "number" || a instanceof Kotlin.Long;
};

Kotlin.isChar = function (value) {
    return (typeof value) == "string" && value.length == 1;
};

Kotlin.isComparable = function (value) {
    var type = typeof value;

    return type === "string" ||
           type === "boolean" ||
           Kotlin.isNumber(value) ||
           Kotlin.isType(value, Kotlin.kotlin.Comparable);
};

Kotlin.isCharSequence = function (value) {
    return typeof value === "string" || Kotlin.isType(value, Kotlin.kotlin.CharSequence);
};

Kotlin.charInc = function (value) {
    return String.fromCharCode(value.charCodeAt(0)+1);
};

Kotlin.charDec = function (value) {
    return String.fromCharCode(value.charCodeAt(0)-1);
};

Kotlin.toShort = function (a) {
    return (a & 0xFFFF) << 16 >> 16;
};

Kotlin.toByte = function (a) {
    return (a & 0xFF) << 24 >> 24;
};

Kotlin.toChar = function (a) {
   return String.fromCharCode((((a | 0) % 65536) & 0xFFFF) << 16 >>> 16);
};

Kotlin.numberToLong = function (a) {
    return a instanceof Kotlin.Long ? a : Kotlin.Long.fromNumber(a);
};

Kotlin.numberToInt = function (a) {
    return a instanceof Kotlin.Long ? a.toInt() : (a | 0);
};

Kotlin.numberToShort = function (a) {
    return Kotlin.toShort(Kotlin.numberToInt(a));
};

Kotlin.numberToByte = function (a) {
    return Kotlin.toByte(Kotlin.numberToInt(a));
};

Kotlin.numberToDouble = function (a) {
    return +a;
};

Kotlin.numberToChar = function (a) {
    return Kotlin.toChar(Kotlin.numberToInt(a));
};

Kotlin.intUpto = function (from, to) {
    return new Kotlin.kotlin.ranges.IntRange(from, to);
};

Kotlin.intDownto = function (from, to) {
    return new Kotlin.kotlin.ranges.IntProgression(from, to, -1);
};

Kotlin.throwNPE = function (message) {
    throw new Kotlin.kotlin.NullPointerException(message);
};

Kotlin.throwCCE = function () {
    throw new Kotlin.kotlin.ClassCastException("Illegal cast");
};

Kotlin.throwISE = function (message) {
    throw new Kotlin.kotlin.IllegalStateException(message);
};

/** @const */
var POW_2_32 = 4294967296;
// TODO: consider switching to Symbol type once we are on ES6.
/** @const */
var OBJECT_HASH_CODE_PROPERTY_NAME = "kotlinHashCodeValue$";

function getObjectHashCode(obj) {
    if (!(OBJECT_HASH_CODE_PROPERTY_NAME in obj)) {
        var hash = (Math.random() * POW_2_32) | 0; // Make 32-bit singed integer.
        Object.defineProperty(obj, OBJECT_HASH_CODE_PROPERTY_NAME, { value:  hash, enumerable: false });
    }
    return obj[OBJECT_HASH_CODE_PROPERTY_NAME];
}

function getStringHashCode(str) {
    var hash = 0;
    for (var i = 0; i < str.length; i++) {
        var code  = str.charCodeAt(i);
        hash  = (hash * 31 + code) | 0; // Keep it 32-bit.
    }
    return hash;
}

Kotlin.safeParseInt = function (str) {
    var r = parseInt(str, 10);
    return isNaN(r) ? null : r;
};

Kotlin.safeParseDouble = function (str) {
    var r = parseFloat(str);
    return isNaN(r) ? null : r;
};

Kotlin.arrayEquals = function (a, b) {
    if (a === b) {
        return true;
    }
    if (!Array.isArray(b) || a.length !== b.length) {
        return false;
    }

    for (var i = 0, n = a.length; i < n; i++) {
        if (!Kotlin.equals(a[i], b[i])) {
            return false;
        }
    }
    return true;
};

Kotlin.arrayDeepEquals = function (a, b) {
    if (a === b) {
        return true;
    }
    if (!Array.isArray(b) || a.length !== b.length) {
        return false;
    }

    for (var i = 0, n = a.length; i < n; i++) {
        if (Array.isArray(a[i])) {
            if (!Kotlin.arrayDeepEquals(a[i], b[i])) {
                return false;
            }
        } else if (!Kotlin.equals(a[i], b[i])) {
            return false;
        }
    }
    return true;
};

Kotlin.arrayHashCode = function (arr) {
    var result = 1;
    for (var i = 0, n = arr.length; i < n; i++) {
        result = ((31 * result | 0) + Kotlin.hashCode(arr[i])) | 0;
    }
    return result;
};

Kotlin.arrayDeepHashCode = function (arr) {
    var result = 1;
    for (var i = 0, n = arr.length; i < n; i++) {
        var e = arr[i];
        result = ((31 * result | 0) + (Array.isArray(e) ? Kotlin.arrayDeepHashCode(e) : Kotlin.hashCode(e))) | 0;
    }
    return result;
};

Kotlin.primitiveArraySort = function(array) {
    array.sort(Kotlin.primitiveCompareTo)
};

Kotlin.nullArray = function (size) {
    var res = [];
    var i = size;
    while (i > 0) {
        res[--i] = null;
    }
    return res;
};

Kotlin.numberArrayOfSize = function (size) {
    return Kotlin.arrayFromFun(size, function () {
        return 0;
    });
};

Kotlin.charArrayOfSize = function (size) {
    return Kotlin.arrayFromFun(size, function () {
        return '\0';
    });
};

Kotlin.booleanArrayOfSize = function (size) {
    return Kotlin.arrayFromFun(size, function () {
        return false;
    });
};

Kotlin.longArrayOfSize = function (size) {
    return Kotlin.arrayFromFun(size, function () {
        return Kotlin.Long.ZERO;
    });
};

Kotlin.arrayFromFun = function (size, initFun) {
    var result = new Array(size);
    for (var i = 0; i < size; i++) {
        result[i] = initFun(i);
    }
    return result;
};

Kotlin.identityHashCode = getObjectHashCode;

