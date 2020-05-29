/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.roots

import com.intellij.openapi.diagnostic.logger

class GradleBuildRootIndex : StandaloneScriptsUpdater {
    private val log = logger<GradleBuildRootIndex>()

    private val standaloneScriptRoots = mutableMapOf<String, GradleBuildRoot?>()

    private val byWorkingDir = HashMap<String, GradleBuildRoot>()
    private val byProjectDir = HashMap<String, GradleBuildRoot>()

    val list: Collection<GradleBuildRoot>
        get() = byWorkingDir.values

    @Synchronized
    fun rebuildProjectRoots() {
        byProjectDir.clear()
        byWorkingDir.values.forEach { buildRoot ->
            buildRoot.projectRoots.forEach {
                byProjectDir[it] = buildRoot
            }
        }

        standaloneScriptRoots.keys.forEach(::computeStandaloneScriptRoot)
    }

    @Synchronized
    fun getBuildByRootDir(dir: String) = byWorkingDir[dir]

    @Synchronized
    fun findNearestRoot(path: String): GradleBuildRoot? {
        var max: Pair<String, GradleBuildRoot>? = null
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
    fun isStandaloneScript(path: String) = path in standaloneScriptRoots

    @Synchronized
    fun getStandaloneScriptRoot(path: String) = standaloneScriptRoots[path]

    @Synchronized
    fun add(value: GradleBuildRoot): GradleBuildRoot? {
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

    @Synchronized
    override fun addStandaloneScript(path: String) {
        computeStandaloneScriptRoot(path)
    }

    @Synchronized
    override fun removeStandaloneScript(path: String) =
        standaloneScriptRoots.remove(path)

    override var standaloneScripts: Collection<String>
        @Synchronized get() = standaloneScriptRoots.keys
        @Synchronized set(value) {
            standaloneScriptRoots.clear()
            value.forEach(::computeStandaloneScriptRoot)
        }

    private fun computeStandaloneScriptRoot(path: String) {
        standaloneScriptRoots[path] = findNearestRoot(path)
    }
}
