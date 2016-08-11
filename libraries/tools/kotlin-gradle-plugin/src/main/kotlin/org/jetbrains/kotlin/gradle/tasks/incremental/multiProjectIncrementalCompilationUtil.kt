package org.jetbrains.kotlin.gradle.tasks.incremental

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.gradle.plugin.kotlinDebug
import org.jetbrains.kotlin.gradle.tasks.ArtifactDifferenceRegistry
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.tryGetSingleArtifact
import java.io.File

fun configureMultiProjectIncrementalCompilation(
        project: Project,
        kotlinTask: KotlinCompile,
        javaTask: JavaCompile,
        kotlinAfterJavaTask: KotlinCompile?,
        artifactDifferenceRegistry: ArtifactDifferenceRegistry
) {
    val log = kotlinTask.logger
    log.kotlinDebug { "Configuring multi-project incremental compilation for project ${project.path}" }

    fun cannotPerformMultiProjectIC(artifact: File, reason: String) {
        log.kotlinDebug {
            "Multi-project kotlin incremental compilation cannot be performed for project ${project.path}: $reason"
        }
        artifactDifferenceRegistry.remove(artifact)
    }

    fun isUnknownTaskOutputtingToJavaDestination(task: Task): Boolean {
        return task !is JavaCompile &&
                task !is KotlinCompile &&
                task is AbstractCompile &&
                FileUtil.isAncestor(javaTask.destinationDir, task.destinationDir, /* strict = */ false)
    }

    val artifact = project.tryGetSingleArtifact() ?: return
    if (!kotlinTask.incremental) {
        return cannotPerformMultiProjectIC(artifact, reason = "incremental compilation is not enabled")
    }

    val illegalTask = project.tasks.find(::isUnknownTaskOutputtingToJavaDestination)
    if (illegalTask != null) {
        return cannotPerformMultiProjectIC(artifact,
                reason = "unknown task outputs to java destination dir ${illegalTask.path} $(${illegalTask.javaClass})")
    }

    val kotlinCompile = kotlinAfterJavaTask ?: kotlinTask
    kotlinCompile.artifactDifferenceRegistry = artifactDifferenceRegistry
}
