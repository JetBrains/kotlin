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

package org.jetbrains.kotlin.incremental

import java.io.File

internal fun File.isJavaFile() =
        extension.equals("java", ignoreCase = true)

internal fun File.isKotlinFile(): Boolean =
    extension.let {
        "kt".equals(it, ignoreCase = true) ||
        "kts".equals(it, ignoreCase = true)
    }

internal fun File.isClassFile(): Boolean =
        extension.equals("class", ignoreCase = true)

internal fun listClassFiles(path: String): Sequence<File> =
        File(path).walk().filter { it.isFile && it.isClassFile() }

internal fun File.relativeOrCanonical(base: File): String =
        relativeToOrNull(base)?.path ?: canonicalPath

internal fun Iterable<File>.pathsAsStringRelativeTo(base: File): String =
        map { it.relativeOrCanonical(base) }.sorted().joinToString()
