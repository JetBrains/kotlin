/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.config

import java.io.File

class LombokConfig(private val config: Map<String, String>) {

    fun getString(key: String): String? = config[key]

    fun getBoolean(key: String): Boolean? = getString(key)?.toBoolean()

    fun getBooleanOrDefault(key: String, default: Boolean = false): Boolean = getBoolean(key) ?: default

    companion object {

        val Empty = LombokConfig(emptyMap())

        fun parse(path: File): LombokConfig {

            val config = mutableMapOf<String, String>()

            path.forEachLine { line ->
                val parts = line.split("=", limit = 2)
                if (parts.size == 2) {
                    config[parts[0].trim()] = parts[1].trim()
                }
            }
            return LombokConfig(config)
        }
    }

}
