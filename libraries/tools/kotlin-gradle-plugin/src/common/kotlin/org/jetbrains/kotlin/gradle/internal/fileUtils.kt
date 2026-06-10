/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import java.io.File
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

fun Path.ensureParentDirsCreated() {
    val parent = parent
    if (!parent.exists()) {
        parent.createDirectories()
    }
}

fun File.ensureParentDirsCreated() {
    toPath().ensureParentDirsCreated()
}
