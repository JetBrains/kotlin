/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.konan.libgit

import kotlinx.cinterop.*
import platform.posix.*
import libgit2.*

fun main(args: Array<String>) {
    if (args.size == 0)
        return help()

    val workDir = args[0]
    val limit = if (args.size > 1) {
        args[1].toInt()
    } else
        null

    println("Opening…")
    val repository = git.repository(workDir)
    val map = mutableMapOf<String, Int>()
    var count = 0
    val commits = repository.commits()
    val limited = limit?.let { commits.take(it) } ?: commits
    println("Calculating…")
    limited.forEach { commit ->
        if (count % 100 == 0)
            println("Commit #$count [${commit.time.format()}]: ${commit.summary}")

        commit.parents.forEach { parent ->
            val diff = commit.tree.diff(parent.tree)
            diff.deltas().forEach { delta ->
                val path = delta.newPath
                val n = map[path] ?: 0
                map.put(path, n + 1)
            }
            diff.close()
            parent.close()
        }
        commit.close()
        count++
    }
    println("Report:")
    map.toList().sortedByDescending { it.second }.take(10).forEach {
        println("File: ${it.first}")
        println("      ${it.second}")
        println()
    }

    repository.close()
    git.close()
}

fun git_time_t.format() = memScoped {
    val commitTime = alloc<time_tVar>()
    commitTime.value = this@format
    ctime(commitTime.ptr)!!.toKString().trim()
}


private fun printTree(commit: GitCommit) {
    commit.tree.entries().forEach { entry ->
        when (entry) {
            is GitTreeEntry.File -> println("     ${entry.name}")
            is GitTreeEntry.Folder -> println("     /${entry.name} (${entry.subtree.entries().size})")
        }
    }
}

fun help() {
    println("Working directory should be provided")
}
