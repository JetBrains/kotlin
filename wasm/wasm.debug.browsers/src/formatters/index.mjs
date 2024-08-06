/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import int from "./int.mjs";
import long from "./long.mjs";
import float from "./float.mjs";
import _null from "./null.mjs";
import _class from "./class.mjs";
import double from "./double.mjs";
import string from "./string.mjs";
import { array, arrayList, hashMap, hashSet } from "./collections.mjs";
import entry, { ENTRY_TYPE } from "./entry.mjs";
import foldedList, { FOLDED_TYPE } from "./folded-list.mjs";
import {
    i8,
    i16,
    i32,
    i64,
    f32,
    f64,
    isRef,
    kotlinArray,
    kotlinString,
    kotlinArrayList,
    kotlinNullableArray,
    kotlinNullableString,
    kotlinNullableArrayList,
    kotlinShortArray,
    kotlinNullableShortArray,
    kotlinUShortArray,
    kotlinNullableUShortArray,
    kotlinByteArray,
    kotlinNullableByteArray,
    kotlinUByteArray,
    kotlinNullableUByteArray,
    kotlinNullableIntArray,
    kotlinIntArray,
    kotlinUIntArray,
    kotlinNullableUIntArray,
    kotlinLongArray,
    kotlinNullableULongArray,
    kotlinULongArray,
    kotlinNullableLongArray,
    kotlinFloatArray,
    kotlinNullableFloatArray,
    kotlinDoubleArray,
    kotlinNullableDoubleArray,
    kotlinHashSet,
    kotlinNullableHashSet,
    kotlinHashMap,
    kotlinNullableHashMap,
} from "../type-checkers/index.mjs";

const simpleFormatters = new Map([
    [ENTRY_TYPE, entry],
    [FOLDED_TYPE, foldedList],
    [kotlinString(), string],
    [kotlinNullableString(), string],
    [i8(), int],
    [i16(), int],
    [i32(), int],
    [i64(), long],
    [f32(), float],
    [f64(), double],
    [kotlinArray(), array],
    [kotlinNullableArray(), array],
    [kotlinArray(), array],
    [kotlinNullableArray(), array],
    [kotlinByteArray(), array],
    [kotlinNullableByteArray(), array],
    [kotlinUByteArray(), array],
    [kotlinNullableUByteArray(), array],
    [kotlinShortArray(), array],
    [kotlinNullableShortArray(), array],
    [kotlinUShortArray(), array],
    [kotlinNullableUShortArray(), array],
    [kotlinIntArray(), array],
    [kotlinNullableIntArray(), array],
    [kotlinUIntArray(), array],
    [kotlinNullableUIntArray(), array],
    [kotlinLongArray(), array],
    [kotlinNullableLongArray(), array],
    [kotlinULongArray(), array],
    [kotlinNullableULongArray(), array],
    [kotlinFloatArray(), array],
    [kotlinNullableFloatArray(), array],
    [kotlinDoubleArray(), array],
    [kotlinNullableDoubleArray(), array],
    [kotlinArrayList(), arrayList],
    [kotlinNullableArrayList(), arrayList],
    [kotlinHashSet(), hashSet],
    [kotlinNullableHashSet(), hashSet],
    [kotlinHashMap(), hashMap],
    [kotlinNullableHashMap(), hashMap],
])

export default {
    get(object) {
        const type = object?.type;
        if (type == null) return undefined

        const simpleFormatter = simpleFormatters.get(type)
        if (simpleFormatter !== undefined) {
            return object.value === null ? _null : simpleFormatter
        }

        if (isRef(type)) {
            return object.value === null ? _null : _class
        }
    }
}