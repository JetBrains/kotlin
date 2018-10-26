/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package sample.gitchurn

import kotlinx.cinterop.*
import libgit2.git_time_t
import platform.posix.ctime
import platform.posix.time_tVar

fun main(args: Array<String>) {
    if (args.isEmpty())
        return help()

    val workDir = args[0]

    val limit = if (args.size > 1) {
        val limitRaw = args[1].toIntOrNull()
        if (limitRaw == null || limitRaw <= 0) {
            return help("Not a positive integer: $limitRaw")
        }
        limitRaw
    } else
        Int.MAX_VALUE

    try {
        calculateChurn(workDir, limit)
    } catch (e: GitException) {
        help(e.message)
    }
}

private fun calculateChurn(workDir: String, limit: Int) {
    println("Opening…")
    val repository = git.repository(workDir)
    val map = mutableMapOf<String, Int>()
    var count = 0
    val commits = repository.commits()
    val limited = commits.take(limit)
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

private fun git_time_t.format() = memScoped {
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

private fun help(errorMessage: String? = null) {
    errorMessage?.let {
        println("ERROR: $it")
    }
    println("./gitchurn.kexe <work dir> [<limit>]")
}
