/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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
    return a instanceof Kotlin.Long ? a.toInt() : Kotlin.doubleToInt(a);
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

Kotlin.doubleToInt = function(a) {
    if (a > 2147483647) return 2147483647;
    if (a < -2147483648) return -2147483648;
    return a | 0;
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
