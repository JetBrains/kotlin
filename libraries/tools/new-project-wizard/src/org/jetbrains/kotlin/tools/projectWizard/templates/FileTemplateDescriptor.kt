package org.jetbrains.kotlin.tools.projectWizard.templates

import org.jetbrains.kotlin.tools.projectWizard.core.Defaults
import org.jetbrains.kotlin.tools.projectWizard.core.div
import java.nio.file.Path


// Should be used to create any kind of files in the generated project
// Except build files as they will be generated using IR
data class FileTemplateDescriptor(val templateId: String, val relativePath: Path)

data class FileTemplate(
    val descriptor: FileTemplateDescriptor,
    val rootPath: Path,
    val data: Map<String, Any?> = emptyMap()
) {
    constructor(descriptor: FileTemplateDescriptor, rootPath: Path, vararg data: Pair<String, Any?>) :
            this(descriptor, rootPath, data.toMap())
}

fun resourcesPath(fileName: String) =
    Defaults.RESOURCES_DIR / fileName

fun sourcesPath(fileName: String) =
    Defaults.KOTLIN_DIR / fileName