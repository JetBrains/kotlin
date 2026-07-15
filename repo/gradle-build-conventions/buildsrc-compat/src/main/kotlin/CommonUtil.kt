// usages in build scripts are not tracked properly
@file:Suppress("unused")

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ProjectLayout
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.internal.os.OperatingSystem
import proguard.gradle.ProGuardTask
import java.io.File

fun fileFrom(root: File, vararg children: String): File = children.fold(root) { f, c -> File(f, c) }

fun fileFrom(root: String, vararg children: String): File = children.fold(File(root)) { f, c -> File(f, c) }

fun Task.singleOutputFile(layout: ProjectLayout): File = when (this) {
    is AbstractArchiveTask -> archiveFile.get().asFile
    is ProGuardTask -> layout.files(outJarFiles.single()!!).singleFile
    else -> outputs.files.singleFile
}

val Project.isConfigurationCacheDisabled
    get() = (gradle.startParameter as? org.gradle.api.internal.StartParameterInternal)?.configurationCache?.get() != true

fun getMvnwCmd(): List<String> = when {
    OperatingSystem.current().isWindows -> listOf("cmd", "/c", "mvnw.cmd")
    else -> listOf("./mvnw")
}
