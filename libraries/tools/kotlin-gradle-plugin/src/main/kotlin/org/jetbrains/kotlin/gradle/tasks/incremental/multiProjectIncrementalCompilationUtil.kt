package org.jetbrains.kotlin.gradle.tasks.incremental

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.gradle.plugin.kotlinDebug
import org.jetbrains.kotlin.gradle.tasks.ArtifactDifferenceRegistry
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

fun configureMultiProjectIncrementalCompilation(
        project: Project,
        kotlinTask: KotlinCompile,
        javaTask: AbstractCompile,
        kotlinAfterJavaTask: KotlinCompile?,
        artifactDifferenceRegistry: ArtifactDifferenceRegistry,
        artifactFile: File?
) {
    val log = kotlinTask.logger
    log.kotlinDebug { "Configuring multi-project incremental compilation for project ${project.path}" }

    fun cannotPerformMultiProjectIC(reason: String) {
        log.kotlinDebug {
            "Multi-project kotlin incremental compilation won't be performed for projects that depend on ${project.path}: $reason"
        }
        artifactFile?.let { artifactDifferenceRegistry.remove(it) }
    }

    fun isUnknownTaskOutputtingToJavaDestination(task: Task): Boolean {
        return task !is JavaCompile &&
                task !is KotlinCompile &&
                task is AbstractCompile &&
                FileUtil.isAncestor(javaTask.destinationDir, task.destinationDir, /* strict = */ false)
    }

    if (!kotlinTask.incremental) {
        return cannotPerformMultiProjectIC(reason = "incremental compilation is not enabled")
    }

    // todo: split registry for reading and writing changes
    val illegalTask = project.tasks.find(::isUnknownTaskOutputtingToJavaDestination)
    if (illegalTask != null) {
        return cannotPerformMultiProjectIC(reason = "unknown task outputs to java destination dir ${illegalTask.path} $(${illegalTask.javaClass})")
    }

    val kotlinCompile = kotlinAfterJavaTask ?: kotlinTask
    kotlinCompile.artifactDifferenceRegistry = artifactDifferenceRegistry
    kotlinCompile.artifactFile = artifactFile
}
