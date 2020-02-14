/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.webpack

import org.gradle.api.tasks.Input
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackLibraryTarget.Companion.fromString

data class KotlinWebpackOutput(
    @Input var library: String?,
    @Input var libraryTarget: KotlinWebpackLibraryTarget?
) {
    fun getLibraryTarget(): String {
        return this.libraryTarget.toString()
    }

    fun setLibraryTarget(value: String) {
        this.libraryTarget = fromString(value)
    }
}

enum class KotlinWebpackLibraryTarget {
    VAR,
    ASSIGN,
    THIS,
    WINDOW,
    SELF,
    GLOBAL,
    COMMONJS,
    COMMONJS2,
    COMMONJS_MODULE,
    AMD,
    AMD_REQUIRE,
    UMD,
    UMD2,
    JSONP,
    SYSTEM;

    override fun toString(): String {
        return super.toString()
            .toLowerCase()
            .replace("_", "-")
    }

    companion object {
        internal fun fromString(value: String): KotlinWebpackLibraryTarget {
            return valueOf(value.toUpperCase().replace("-", "_"))
        }
    }
}