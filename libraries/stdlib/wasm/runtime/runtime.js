/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

const runtime = {
    /**
     * kotlin.String#plus(other: Any?): String
     * @return {string}
     */
    String_plus(str1, str2) {
        if (typeof str1 != "string") throw `Illegal argument str1: ${str1}`;
        return str1 + String(str2);
    },

    /**
     * @return {number}
     */
    String_getLength(str) {
        if (typeof str != "string") throw `Illegal argument x: ${str}`;
        return str.length;
    },

    /**
     * @return {number}
     */
    String_getChar(str, index) {
        if (typeof str != "string") throw `Illegal argument str: ${str}`;
        if (typeof index != "number") throw `Illegal argument index: ${index}`;
        return str.charCodeAt(index);
    },

    /**
     * @return {number}
     */
    String_compareTo(str1, str2) {
        if (str1 > str2) return 1;
        if (str1 < str2) return -1;
        return 0;
    },

    /**
     * @return {boolean}
     */
    String_equals(str, other) {
        // if (typeof str != "string") throw `Illegal argument str: ${str}`;
        return str === other;
    },

    /**
     * @return {string}
     */
    String_subsequence(str, startIndex, endIndex) {
        return str.substring(startIndex, endIndex);
    },
    
    String_getLiteral(index) {
        return runtime.stringLiterals[index];
    },

    coerceToString(value) {
        return String(value);
    },

    /**
     * @return {string}
     */
    Char_toString(char) {
        return String.fromCharCode(char)
    },

    JsArray_new(size) {
        if (typeof size != "number") throw `Illegal argument size: ${size}`;
        return new Array(size);
    },

    JsArray_get(array, index) {
        if (typeof index != "number") throw `Illegal argument index: ${index}`;
        if (array.length <= index) throw `Index out of bounds: index=${index} length=${array.length}`;
        return array[index];
    },

    JsArray_set(array, index, value) {
        if (typeof index != "number") throw `Illegal argument index: ${index}`;
        if (array.length <= index) throw `Index out of bounds: index=${index} length=${array.length}`;
        array[index] = value;
    },

    JsArray_getSize(array) {
        return array.length;
    },

    identity(x) {
        return x;
    },

    println(value) {
        console.log(">>>  " + value)
    }
};