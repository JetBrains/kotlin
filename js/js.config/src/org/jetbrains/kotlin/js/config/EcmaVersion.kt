/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.config

@Suppress("EnumEntryName")
enum class EcmaVersion {
    es5, es2015, es2020;

    companion object {
        fun defaultVersion(): EcmaVersion {
            return es5
        }

        fun latestSupportedVersion(): EcmaVersion {
            return es2020
        }

        fun fromName(name: String): EcmaVersion? {
            return entries.firstOrNull { it.name == name }
        }
    }
}

val EcmaVersion?.supportsEsModules: Boolean
    get() = this != null && this >= EcmaVersion.es2015

val EcmaVersion?.supportsPerFileGranularity: Boolean
    get() = this != null && this >= EcmaVersion.es2015
