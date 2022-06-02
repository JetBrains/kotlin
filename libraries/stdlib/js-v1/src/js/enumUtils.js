/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

Kotlin.defaultEnumValueOf = function (enumClass, name) {
    if (typeof enumClass.valueOf == "function" && enumClass.valueOf.length === 1) {
        return enumClass.valueOf(name);
    } else {
        return enumClass[name];
    }
}

Kotlin.defaultEnumValues = function (enumClass) {
    if (typeof enumClass.values == "function" && enumClass.values.length === 0) {
        return enumClass.values();
    } else {
        return Object.keys(enumClass).map(function (key) { return enumClass[key] });
    }
}
