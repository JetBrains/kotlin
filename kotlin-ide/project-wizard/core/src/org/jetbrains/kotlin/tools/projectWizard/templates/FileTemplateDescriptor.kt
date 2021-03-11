package org.jetbrains.kotlin.tools.projectWizard.templates

import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.tools.projectWizard.core.asPath
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.SourcesetType
import java.nio.file.Path
import java.nio.file.Paths

// Should be used to create any kind of files in the generated project
// Except build files as they will be generated using IR
sealed class FileDescriptor {
    abstract val relativePath: Path?
}

data class FileTemplateDescriptor(@NonNls val templateId: String, override val relativePath: Path?) : FileDescriptor() {
    constructor(templateId: String) : this(
        templateId,
        Paths.get(templateId).fileName.toString().removeSuffix(".vm").asPath()
    )
}

data class FileTextDescriptor(val text: String, override val relativePath: Path) : FileDescriptor()

sealed class FilePath {
    abstract val sourcesetType: SourcesetType
}

data class SrcFilePath(override val sourcesetType: SourcesetType) : FilePath()
data class ResourcesFilePath(override val sourcesetType: SourcesetType) : FilePath()

data class FileTemplateDescriptorWithPath(
    val descriptor: FileDescriptor,
    val path: FilePath,
    val data: Map<String, Any> = emptyMap(),
)

infix fun FileDescriptor.asResourceOf(sourcesetType: SourcesetType) =
    FileTemplateDescriptorWithPath(this, ResourcesFilePath(sourcesetType))

infix fun FileDescriptor.asSrcOf(sourcesetType: SourcesetType) =
    FileTemplateDescriptorWithPath(this, SrcFilePath(sourcesetType))

infix fun FileTemplateDescriptorWithPath.withSettings(setting: Pair<String, String>) =
    copy(data = data + setting)


data class FileTemplate(
    val descriptor: FileDescriptor,
    val rootPath: Path,
    val data: Map<String, Any?> = emptyMap()
) {
    constructor(descriptor: FileDescriptor, rootPath: Path, vararg data: Pair<String, Any?>) :
            this(descriptor, rootPath, data.toMap())
}