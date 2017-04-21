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

Kotlin.equals = function (obj1, obj2) {
    if (obj1 == null) {
        return obj2 == null;
    }

    if (obj2 == null) {
        return false;
    }

    if (obj1 !== obj1) {
        return obj2 !== obj2;
    }

    if (typeof obj1 === "object" && typeof obj1.equals === "function") {
        return obj1.equals(obj2);
    }

    return obj1 === obj2;
};

Kotlin.hashCode = function (obj) {
    if (obj == null) {
        return 0;
    }
    var objType = typeof obj;
    if ("object" === objType) {
        return "function" === typeof obj.hashCode ? obj.hashCode() : getObjectHashCode(obj);
    }
    if ("function" === objType) {
        return getObjectHashCode(obj);
    }
    if ("number" === objType) {
        return numberHashCode(obj);
    }
    if ("boolean" === objType) {
        return Number(obj)
    }

    var str = String(obj);
    return getStringHashCode(str);
};

var numberHashCode;

if (typeof ArrayBuffer === "function") {
    var bufferForNumberConversion = new ArrayBuffer(8);
    var arrayForDoubleConversion = new Float64Array(bufferForNumberConversion);
    var arrayForIntegerConversion = new Int32Array(bufferForNumberConversion);

    // Detect endiannes of ArrayBuffer
    var lowerIntegerIndex = 0;
    var upperIntegerIndex = 1;
    (function() {
        arrayForDoubleConversion[0] = 1.2;
        if (arrayForIntegerConversion[0] !== 0x3FF33333) {
            lowerIntegerIndex = 1;
            upperIntegerIndex = 0;
        }
    })();
    numberHashCode = function(obj) {
        if ((obj | 0) === obj) {
            return obj | 0;
        }
        else {
            arrayForDoubleConversion[0] = obj;
            return (arrayForIntegerConversion[lowerIntegerIndex] * 31 | 0) + arrayForIntegerConversion[upperIntegerIndex] | 0;
        }
    }
}
else {
    numberHashCode = function(obj) {
        return obj | 0;
    }
}

Kotlin.toString = function (o) {
    if (o == null) {
        return "null";
    }
    else if (Kotlin.isArrayish(o)) {
        return "[...]";
    }
    else {
        return o.toString();
    }
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

Kotlin.identityHashCode = getObjectHashCode;
