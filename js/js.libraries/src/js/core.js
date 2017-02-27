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
