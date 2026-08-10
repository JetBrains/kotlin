@file:Suppress("unused")
@file:JvmName("AddEmbeddedRuntime")

import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.file.ArchiveOperations
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.support.serviceOf

@JvmOverloads
fun Jar.addEmbeddedRuntime(embeddedConfigurationName: String = "embedded") {
    project.configurations.findByName(embeddedConfigurationName)?.let { embedded ->
        val archiveOperations = project.serviceOf<ArchiveOperations>()
        from(embedded.elements.map { dependencies ->
            dependencies.map { dependency ->
                val dependencyFile = dependency.asFile
                check(!dependencyFile.path.contains("kotlin-stdlib")) {
                    """
                    |There's an attempt to have an embedded kotlin-stdlib in $project which is likely a misconfiguration
                    |All embedded dependencies:
                    |    ${dependencies.joinToString(separator = "\n|    ") { it.asFile.path }}
                    """.trimMargin()
                }

                if (dependencyFile.extension.equals("jar", ignoreCase = true)) {
                    archiveOperations.zipTree(dependency)
                } else {
                    dependency
                }
            }
        })
        val version = project.version.toString()
        rename { filename ->
            if (filename.endsWith(".klib")) filename.removeSuffix(".klib").removeSuffix("-$version") + ".klib" else filename
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