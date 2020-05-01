/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.roots

import com.intellij.openapi.diagnostic.logger

class GradleBuildRootIndex {
    private val log = logger<GradleBuildRootIndex>()

    private val byWorkingDir = HashMap<String, GradleBuildRoot.Linked>()
    val byProjectDir = HashMap<String, GradleBuildRoot.Linked>()

    val values: Collection<GradleBuildRoot>
        get() = byWorkingDir.values

    @Synchronized
    fun rebuildProjectRoots() {
        byProjectDir.clear()
        byWorkingDir.values.forEach { buildRoot ->
            buildRoot.projectRoots.forEach {
                byProjectDir[it] = buildRoot
            }
        }
    }

    @Synchronized
    fun getBuildRoot(dir: String) = byWorkingDir[dir]

    @Synchronized
    fun findNearestRoot(path: String): GradleBuildRoot.Linked? {
        var max: Pair<String, GradleBuildRoot.Linked>? = null
        byWorkingDir.entries.forEach {
            if (path.startsWith(it.key) && (max == null || it.key.length > max!!.first.length)) {
                max = it.key to it.value
            }
        }
        return max?.second
    }

    @Synchronized
    fun getBuildByProjectDir(projectDir: String) = byProjectDir[projectDir]

    @Synchronized
    fun add(value: GradleBuildRoot.Linked): GradleBuildRoot.Linked? {
        val prefix = value.pathPrefix
        val old = byWorkingDir.put(prefix, value)
        rebuildProjectRoots()
        log.info("$prefix: $old -> $value")
        return old
    }

    @Synchronized
    fun remove(prefix: String) = byWorkingDir.remove(prefix)?.also {
        rebuildProjectRoots()
        log.info("$prefix: removed")
    }
}
