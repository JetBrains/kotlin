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

Kotlin.toShort = function (a) {
    return (a & 0xFFFF) << 16 >> 16;
};

Kotlin.toByte = function (a) {
    return (a & 0xFF) << 24 >> 24;
};

Kotlin.toChar = function (a) {
    return a & 0xFFFF;
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

Kotlin.toBoxedChar = function (a) {
    if (a == null) return a;
    if (a instanceof Kotlin.BoxedChar) return a;
    return new Kotlin.BoxedChar(a);
};

Kotlin.unboxChar = function(a) {
    if (a == null) return a;
    return Kotlin.toChar(a);
};
