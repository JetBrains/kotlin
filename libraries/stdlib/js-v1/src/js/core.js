/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors. 
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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

    if (typeof obj1 === "number" && typeof obj2 === "number") {
        return obj1 === obj2 && (obj1 !== 0 || 1 / obj1 === 1 / obj2)
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
        return Kotlin.numberHashCode(obj);
    }
    if ("boolean" === objType) {
        return obj ? 1231 : 1237;
    }

    var str = String(obj);
    return getStringHashCode(str);
};


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
