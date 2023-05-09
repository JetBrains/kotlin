/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.gradle.api.Project
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

fun Set<File>.relativeTo(project: Project): Set<File> = map { it.relativeTo(project.projectDir) }.toSet()

val resourcesRoot: Path
    get() = Paths.get("src", "functionalTest", "resources")
