/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import {
    KOTLIN_ARRAY_FQN,
    KOTLIN_ARRAY_LIST_FQN,
    KOTLIN_BYTE_ARRAY_FQN,
    KOTLIN_DOUBLE_ARRAY_FQN,
    KOTLIN_FLOAT_ARRAY_FQN, KOTLIN_HASH_MAP_FQN, KOTLIN_HASH_SET_FQN,
    KOTLIN_INT_ARRAY_FQN,
    KOTLIN_LONG_ARRAY_FQN,
    KOTLIN_SHORT_ARRAY_FQN,
    KOTLIN_STRING_FQN,
    KOTLIN_UBYTE_ARRAY_FQN,
    KOTLIN_UINT_ARRAY_FQN,
    KOTLIN_ULONG_ARRAY_FQN,
    KOTLIN_USHORT_ARRAY_FQN
} from "./kotlin-fully-qualified-names.mjs";

const REF_PREFIX = "(ref "
const NULL_PREFIX = "null "

function ref(fqn, nullable = false) {
    return `${REF_PREFIX}${nullable ? NULL_PREFIX : ""}$${fqn})`
}

export function isRef(type) {
    return type.startsWith(REF_PREFIX)
}

export function getFqnNameFrom(refType) {
    if (!isRef(refType)) return null;

    const startPosition = refType.startsWith(NULL_PREFIX, REF_PREFIX.length)
        ? REF_PREFIX.length + NULL_PREFIX.length
        : REF_PREFIX.length;

    return refType.substring(startPosition + 1, refType.length - 1);
}

export function kotlinString() {
    return ref(KOTLIN_STRING_FQN);
}

export function kotlinArray() {
    return ref(KOTLIN_ARRAY_FQN);
}

export function kotlinNullableArray() {
    return ref(KOTLIN_ARRAY_FQN, true);
}

export function kotlinByteArray() {
    return ref(KOTLIN_BYTE_ARRAY_FQN);
}

export function kotlinNullableByteArray() {
    return ref(KOTLIN_BYTE_ARRAY_FQN, true);
}

export function kotlinUByteArray() {
    return ref(KOTLIN_UBYTE_ARRAY_FQN);
}

export function kotlinNullableUByteArray() {
    return ref(KOTLIN_UBYTE_ARRAY_FQN, true);
}

export function kotlinShortArray() {
    return ref(KOTLIN_SHORT_ARRAY_FQN);
}

export function kotlinNullableShortArray() {
    return ref(KOTLIN_SHORT_ARRAY_FQN, true);
}

export function kotlinUShortArray() {
    return ref(KOTLIN_USHORT_ARRAY_FQN);
}

export function kotlinNullableUShortArray() {
    return ref(KOTLIN_USHORT_ARRAY_FQN, true);
}

export function kotlinIntArray() {
    return ref(KOTLIN_INT_ARRAY_FQN);
}

export function kotlinNullableIntArray() {
    return ref(KOTLIN_INT_ARRAY_FQN, true);
}

export function kotlinUIntArray() {
    return ref(KOTLIN_UINT_ARRAY_FQN);
}

export function kotlinNullableUIntArray() {
    return ref(KOTLIN_UINT_ARRAY_FQN, true);
}

export function kotlinLongArray() {
    return ref(KOTLIN_LONG_ARRAY_FQN);
}

export function kotlinNullableLongArray() {
    return ref(KOTLIN_LONG_ARRAY_FQN, true);
}

export function kotlinULongArray() {
    return ref(KOTLIN_ULONG_ARRAY_FQN);
}

export function kotlinNullableULongArray() {
    return ref(KOTLIN_ULONG_ARRAY_FQN, true);
}

export function kotlinFloatArray() {
    return ref(KOTLIN_FLOAT_ARRAY_FQN);
}

export function kotlinNullableFloatArray() {
    return ref(KOTLIN_FLOAT_ARRAY_FQN, true);
}

export function kotlinDoubleArray() {
    return ref(KOTLIN_DOUBLE_ARRAY_FQN);
}

export function kotlinNullableDoubleArray() {
    return ref(KOTLIN_DOUBLE_ARRAY_FQN, true);
}

export function kotlinArrayList() {
    return ref(KOTLIN_ARRAY_LIST_FQN);
}

export function kotlinNullableArrayList() {
    return ref(KOTLIN_ARRAY_LIST_FQN, true);
}

export function kotlinHashSet() {
    return ref(KOTLIN_HASH_SET_FQN);
}

export function kotlinNullableHashSet() {
    return ref(KOTLIN_HASH_SET_FQN, true);
}

export function kotlinHashMap() {
    return ref(KOTLIN_HASH_MAP_FQN);
}

export function kotlinNullableHashMap() {
    return ref(KOTLIN_HASH_MAP_FQN, true);
}

export function kotlinNullableString() {
    return ref(KOTLIN_STRING_FQN, true);
}

export function i8() {
    return "i8"
}

export function i16() {
    return "i16"
}

export function i32() {
    return "i32"
}

export function i64() {
    return "i64"
}

export function f32() {
    return "f32"
}

export function f64() {
    return "f64"
}
