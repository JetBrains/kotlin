/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.npm.shared

import org.jetbrains.kotlin.gradle.internal.json.KgpJson
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJsonDependencies
import org.jetbrains.kotlin.gradle.targets.js.npm.isCompatibleArchive
import java.io.File
import java.util.zip.ZipFile

/** Reads the `package.json` of a klib, an unpacked klib or a bare `package.json`, or `null` if there is none. */
// KT-89806 Remove once klibs no longer contain package.json.
internal fun File.readPackageJsonDependencies(): PackageJsonDependencies? =
    packageJsonText()?.let { KgpJson.default.decodeFromString<PackageJsonDependencies>(it) }

private fun File.packageJsonText(): String? = when {
    isFile && name == NpmProject.PACKAGE_JSON -> readText()
    isDirectory -> resolve(NpmProject.PACKAGE_JSON).takeIf { it.isFile }?.readText()
    isCompatibleArchive -> ZipFile(this).use { zip ->
        val entry = zip.getEntry(NpmProject.PACKAGE_JSON) ?: return null
        return zip.getInputStream(entry).reader().use { reader -> reader.readText() }
    }
    else -> null
}
