package org.jetbrains.kotlin.tools.projectWizard.templates

import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.tools.projectWizard.core.asPath
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.SourcesetType
import java.nio.file.Path
import java.nio.file.Paths


// Should be used to create any kind of files in the generated project
// Except build files as they will be generated using IR
data class FileTemplateDescriptor(@NonNls val templateId: String, val relativePath: Path) {
    constructor(templateId: String) : this(
        templateId,
        Paths.get(templateId).fileName.toString().removeSuffix(".vm").asPath()
    )
}

sealed class FilePath {
    abstract val sourcesetType: SourcesetType
}

data class SrcFilePath(override val sourcesetType: SourcesetType) : FilePath()
data class ResourcesFilePath(override val sourcesetType: SourcesetType) : FilePath()

data class FileTemplateDescriptorWithPath(val descriptor: FileTemplateDescriptor, val path: FilePath)

infix fun FileTemplateDescriptor.asResourceOf(sourcesetType: SourcesetType) =
    FileTemplateDescriptorWithPath(this, ResourcesFilePath(sourcesetType))

infix fun FileTemplateDescriptor.asSrcOf(sourcesetType: SourcesetType) =
    FileTemplateDescriptorWithPath(this, SrcFilePath(sourcesetType))


data class FileTemplate(
    val descriptor: FileTemplateDescriptor,
    val rootPath: Path,
    val data: Map<String, Any?> = emptyMap()
) {
    constructor(descriptor: FileTemplateDescriptor, rootPath: Path, vararg data: Pair<String, Any?>) :
            this(descriptor, rootPath, data.toMap())
}