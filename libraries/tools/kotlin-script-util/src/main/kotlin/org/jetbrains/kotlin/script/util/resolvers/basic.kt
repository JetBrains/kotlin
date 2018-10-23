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

package org.jetbrains.kotlin.script.util.resolvers

import org.jetbrains.kotlin.script.util.DependsOn
import org.jetbrains.kotlin.script.util.Repository
import java.io.File
import java.net.MalformedURLException
import java.net.URL

interface Resolver {
    fun tryResolve(dependsOn: DependsOn): Iterable<File>?
    fun tryAddRepo(annotation: Repository): Boolean
}

class DirectResolver : Resolver {
    override fun tryResolve(dependsOn: DependsOn): Iterable<File>? =
        dependsOn.value.takeUnless(String::isBlank)?.let(::File)?.takeIf { it.exists() && (it.isFile || it.isDirectory) }?.let { listOf(it) }

    override fun tryAddRepo(annotation: Repository): Boolean = false
}

class FlatLibDirectoryResolver(vararg paths: File) : Resolver {

    private val localRepos = arrayListOf<File>()

    init {
        for (path in paths) {
            if (!path.exists() || !path.isDirectory) throw IllegalArgumentException("Invalid flat lib directory repository path '$path'")
        }
        localRepos.addAll(paths)
    }

    override fun tryResolve(dependsOn: DependsOn): Iterable<File>? {
        for (path in localRepos) {
            // TODO: add coordinates and wildcard matching
            val res = dependsOn.value.takeUnless(String::isBlank)
                ?.let { File(path, it) }
                ?.takeIf { it.exists() && (it.isFile || it.isDirectory) }
            if (res != null) return listOf(res)
        }
        return null
    }

    override fun tryAddRepo(annotation: Repository): Boolean {
        val urlStr = annotation.url.takeIf { it.isNotBlank() } ?: annotation.value.takeIf { it.isNotBlank() } ?: return false
        val dirFromUrl = urlStr.toRepositoryUrlOrNull()?.takeIf { it.protocol == "file" }?.path
        val repoDir = (dirFromUrl ?: urlStr).toRepositoryFileOrNull() ?: return false
        localRepos.add(repoDir)
        return true
    }

    companion object {
        fun tryCreate(annotation: Repository): FlatLibDirectoryResolver? =
            annotation.value.takeUnless(String::isBlank)?.let(::File)?.takeIf { it.exists() && it.isDirectory }
                ?.let { FlatLibDirectoryResolver(it) }
    }
}

internal fun String.toRepositoryUrlOrNull(): URL? =
    try {
        URL(this)
    } catch (_: MalformedURLException) {
        null
    }

internal fun String.toRepositoryFileOrNull(): File? =
    File(this).takeIf { it.exists() && it.isDirectory }
