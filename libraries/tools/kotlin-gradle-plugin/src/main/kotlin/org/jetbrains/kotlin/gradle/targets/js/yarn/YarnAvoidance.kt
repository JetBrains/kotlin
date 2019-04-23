/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.internal.hash.FileHasher
import org.jetbrains.kotlin.daemon.common.toHexString
import org.jetbrains.kotlin.gradle.internal.ensureParentDirsCreated
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.yarn.Yarn.packageJsonHashFile

class YarnAvoidance(val project: Project) {
    private val prevHash by lazy {
        val packageJsonHashFile = project.packageJsonHashFile
        if (packageJsonHashFile.exists()) packageJsonHashFile.readText() else null
    }

    private val npmProject = project.npmProject
    private val packageJsonFile = npmProject.packageJsonFile

    private val hash by lazy {
        val hasher = (project as ProjectInternal).services.get(FileHasher::class.java)
        hasher.hash(packageJsonFile).toByteArray().toHexString()
    }

    val upToDate: Boolean
        get() = packageJsonFile.exists() &&
                npmProject.nodeModulesDir.isDirectory &&
                prevHash == hash

    fun commit() {
        if (!upToDate) {
            val packageJsonHashFile = project.packageJsonHashFile
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