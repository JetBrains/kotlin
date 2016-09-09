/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import java.io.File
import java.lang.IllegalArgumentException

interface Resolver {
    fun tryResolve(dependsOn: DependsOn): Iterable<File>?
}

class DirectResolver : Resolver {
    override fun tryResolve(dependsOn: DependsOn): Iterable<File>? =
            if (dependsOn.value.isNotBlank() && File(dependsOn.value).exists()) listOf(File(dependsOn.value)) else null
}

class FlatLibDirectoryResolver(val path: File) : Resolver {

    init {
        if (!path.exists() || !path.isDirectory) throw IllegalArgumentException("Invalid flat lib directory repository path '$path'")
    }

    override fun tryResolve(dependsOn: DependsOn): Iterable<File>? =
            when {
                dependsOn.value.isNotBlank() && File(path, dependsOn.value).exists() -> listOf(File(path, dependsOn.value))
                // TODO: add coordinates and wildcard matching
                else -> null
            }
}
