package org.jetbrains.kotlin.idea.completion

import com.intellij.internal.statistic.collectors.fus.fileTypes.FileTypeUsageSchemaDescriptor
import com.intellij.openapi.vfs.VirtualFile

class KotlinFileTypeSchemaDetector : FileTypeUsageSchemaDescriptor {
    override fun describes(file: VirtualFile): Boolean =
        file.name.endsWith(".kt")
}

class KotlinScriptFileTypeSchemaDetector : FileTypeUsageSchemaDescriptor {
    override fun describes(file: VirtualFile): Boolean =
        !file.name.endsWith("gradle.kts") && file.name.endsWith(".kts")
}