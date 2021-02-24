/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.internal.hash.FileHasher
import org.gradle.internal.service.ServiceRegistry
import org.jetbrains.kotlin.daemon.common.toHexString
import org.jetbrains.kotlin.gradle.internal.ensureParentDirsCreated
import java.io.File

class PackageJsonUpToDateCheck(val services: ServiceRegistry, val npmProject: NpmProject) {

    private val NpmProject.packageJsonHashFile: File
        get() = dir.resolve("package.json.hash")

    private val prevHash by lazy {
        val packageJsonHashFile = npmProject.packageJsonHashFile
        if (packageJsonHashFile.exists()) packageJsonHashFile.readText() else null
    }

    private val packageJsonFile = npmProject.packageJsonFile

    private val hash by lazy {
        val hasher = services.get(FileHasher::class.java)
        hasher.hash(packageJsonFile).toByteArray().toHexString()
    }

    val upToDate: Boolean
        get() = packageJsonFile.exists() &&
                npmProject.nodeModulesDir.isDirectory &&
                prevHash == hash

    fun commit() {
        if (!upToDate) {
            val packageJsonHashFile = npmProject.packageJsonHashFile
            packageJsonHashFile.ensureParentDirsCreated()
            packageJsonHashFile.writeText(hash)
        }
    }

    inline fun updateIfNeeded(body: () -> Unit) {
        if (!upToDate) {
            body()
            commit()
        }
    }
}