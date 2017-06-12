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
