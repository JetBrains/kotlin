/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import java.io.File

@Deprecated("Unused KGP utility. Scheduled for removal in Kotlin 2.7.")
fun File.ensureParentDirsCreated() {
    val parentFile = parentFile
    if (!parentFile.exists()) {
        check(parentFile.mkdirs()) {
            "Cannot create parent directories for $this"
        }
    }
}
