@file:Suppress("unused")
@file:JvmName("AddEmbeddedRuntime")

import org.gradle.api.Project
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.CopySourceSpec
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.support.serviceOf
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetContainer
import java.io.File
import java.util.concurrent.Callable

@JvmOverloads
fun Jar.addEmbeddedRuntime(embeddedConfigurationName: String = "embedded") {
    project.configurations.findByName(embeddedConfigurationName)?.let { embedded ->
        dependsOn(embedded)
        val archiveOperations = project.serviceOf<ArchiveOperations>()
        from {
            embedded.map { dependency: File ->
                check(!dependency.path.contains("kotlin-stdlib")) {
                    """
                    |There's an attempt to have an embedded kotlin-stdlib in $project which is likely a misconfiguration
                    |All embedded dependencies:
                    |    ${embedded.files.joinToString(separator = "\n|    ")}
                    """.trimMargin()
                }

                if (dependency.extension.equals("jar", ignoreCase = true)) {
                    archiveOperations.zipTree(dependency)
                } else {
                    dependency
                }
            }
        }
    }
}

@JvmOverloads
fun Jar.addEmbeddedSources(configurationName: String = "embedded") {
    project.configurations.findByName(configurationName)?.let { embedded ->
        val allSources by lazy {
            embedded.resolvedConfiguration
                .resolvedArtifacts
                .map { it.id.componentIdentifier }
                .filterIsInstance<ProjectComponentIdentifier>()
                .mapNotNull {
                    project.project(it.projectPath).sources()
                }
        }
        from({ allSources })
    }
}