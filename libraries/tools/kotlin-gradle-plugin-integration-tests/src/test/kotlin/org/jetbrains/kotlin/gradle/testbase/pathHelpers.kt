/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.asSequence

/**
 * Find the file with given [name] in current [Path].
 *
 * @return `null` if file is absent in current [Path]
 */
internal fun Path.findInPath(name: String): Path? = Files
    .walk(this)
    .asSequence()
    .find { it.fileName.toString() == name }
