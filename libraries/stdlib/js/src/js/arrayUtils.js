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

Kotlin.isBooleanArray = function (a) {
    return (Array.isArray(a) || a instanceof Int8Array) && a.$type$ === "BooleanArray"
};

Kotlin.isByteArray = function (a) {
    return a instanceof Int8Array && a.$type$ !== "BooleanArray"
};

Kotlin.isShortArray = function (a) {
    return a instanceof Int16Array
};

Kotlin.isCharArray = function (a) {
    return a instanceof Uint16Array && a.$type$ === "CharArray"
};

Kotlin.isIntArray = function (a) {
    return a instanceof Int32Array
};

Kotlin.isFloatArray = function (a) {
    return a instanceof Float32Array
};

Kotlin.isDoubleArray = function (a) {
    return a instanceof Float64Array
};

Kotlin.isLongArray = function (a) {
    return Array.isArray(a) && a.$type$ === "LongArray"
};

Kotlin.isArray = function (a) {
    return Array.isArray(a) && !a.$type$;
};

Kotlin.isArrayish = function (a) {
    return Array.isArray(a) || ArrayBuffer.isView(a)
};

Kotlin.arrayToString = function (a) {
    var toString = Kotlin.isCharArray(a) ? String.fromCharCode : Kotlin.toString;
    return "[" + Array.prototype.map.call(a, function(e) { return toString(e); }).join(", ") + "]";
};

Kotlin.arrayDeepToString = function (a, visited) {
    visited = visited || [a];
    var toString = Kotlin.isCharArray(a) ? String.fromCharCode : Kotlin.toString;
    return "[" + Array.prototype.map.call(a, function (e) {
            if (Kotlin.isArrayish(e) && visited.indexOf(e) < 0) {
                visited.push(e);
                var result = Kotlin.arrayDeepToString(e, visited);
                visited.pop();
                return result;
            }
            else {
                return toString(e);
            }
        }).join(", ") + "]";
};

Kotlin.arrayEquals = function (a, b) {
    if (a === b) {
        return true;
    }
    if (!Kotlin.isArrayish(b) || a.length !== b.length) {
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
    if (!Kotlin.isArrayish(b) || a.length !== b.length) {
        return false;
    }

    for (var i = 0, n = a.length; i < n; i++) {
        if (Kotlin.isArrayish(a[i])) {
            if (!Kotlin.arrayDeepEquals(a[i], b[i])) {
                return false;
            }
        }
        else if (!Kotlin.equals(a[i], b[i])) {
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
        result = ((31 * result | 0) + (Kotlin.isArrayish(e) ? Kotlin.arrayDeepHashCode(e) : Kotlin.hashCode(e))) | 0;
    }
    return result;
};

Kotlin.primitiveArraySort = function (array) {
    array.sort(Kotlin.doubleCompareTo)
};
