/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder

internal object JsonUtils {
    internal val gson: Gson by lazy { GsonBuilder().setLenient().setPrettyPrinting().serializeNulls().create() }

    internal fun <K, V> toMap(jsonText: String): Map<K, V> {
        return gson.fromJson<Map<K, V>>(jsonText, Map::class.java)
    }
}