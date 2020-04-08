/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script

import com.intellij.internal.statistic.collectors.fus.fileTypes.FileTypeUsageSchemaDescriptor
import com.intellij.openapi.vfs.VirtualFile

class KotlinGradleScriptFileTypeSchemaDetector : FileTypeUsageSchemaDescriptor {
    override fun describes(file: VirtualFile): Boolean =
        file.name.endsWith(".gradle.kts")
}