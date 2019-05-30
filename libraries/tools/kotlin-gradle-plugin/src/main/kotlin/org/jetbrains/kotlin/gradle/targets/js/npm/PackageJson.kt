/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import com.google.gson.GsonBuilder
import org.jetbrains.kotlin.gradle.internal.ensureParentDirsCreated
import java.io.File

class PackageJson(
    var name: String,
    var version: String
) {
    val empty: Boolean
        get() = main == null &&
                private == null &&
                workspaces == null &&
                dependencies.isEmpty() &&
                devDependencies.isEmpty()

    val scopedName: ScopedName
        get() = scopedName(name)

    var private: Boolean? = null

    var main: String? = null

    var workspaces: Collection<String>? = null

    val devDependencies = mutableMapOf<String, String>()
    val dependencies = mutableMapOf<String, String>()
    val peerDependencies = mutableMapOf<String, String>()
    val optionalDependencies = mutableMapOf<String, String>()
    val bundledDependencies = mutableListOf<String>()

    companion object {
        fun scopedName(name: String): ScopedName = if (name.contains("/")) ScopedName(
            scope = name.substringBeforeLast("/").removePrefix("@"),
            name = name.substringAfterLast("/")
        ) else ScopedName(scope = null, name = name)

        operator fun invoke(scope: String, name: String, version: String) =
            PackageJson(ScopedName(scope, name).toString(), version)
    }

    data class ScopedName(val scope: String?, val name: String) {
        override fun toString() = if (scope == null) name else "@$scope/$name"
    }

    fun saveTo(packageJsonFile: File) {
        val gson = GsonBuilder()
            .setPrettyPrinting()
            .create()

        packageJsonFile.ensureParentDirsCreated()
        packageJsonFile.writer().use {
            gson.toJson(this, it)
        }
    }
}