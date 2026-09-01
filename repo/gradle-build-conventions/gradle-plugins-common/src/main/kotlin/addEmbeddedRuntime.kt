@file:Suppress("unused")
@file:JvmName("AddEmbeddedRuntime")

import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.DocsType
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.support.serviceOf

@JvmOverloads
fun Jar.addEmbeddedRuntime(embeddedConfigurationName: String = "embedded") {
    val projectPath = project.path
    project.configurations.findByName(embeddedConfigurationName)?.let { embedded ->
        val archiveOperations = project.serviceOf<ArchiveOperations>()
        from(embedded.elements.map { dependencies ->
            dependencies.map { dependency ->
                val dependencyFile = dependency.asFile
                check(!dependencyFile.path.contains("kotlin-stdlib")) {
                    """
                    |There's an attempt to have an embedded kotlin-stdlib in $projectPath which is likely a misconfiguration
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
        val archiveOperations = project.serviceOf<ArchiveOperations>()
        val objects = project.objects

        val jarArtifactsView = embedded.incoming.artifactView {
            isLenient = true
            attributes {
                attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category::class.java, Category.DOCUMENTATION))
                attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType::class.java, DocsType.SOURCES))
            }
            withVariantReselection()
            componentFilter { it is ProjectComponentIdentifier }
        }

        val sourceDirectoriesView = embedded.incoming.artifactView {
            isLenient = true
            attributes {
                attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category::class.java, "verification"))
                attribute(Attribute.of("org.gradle.verificationtype", String::class.java), "main-sources")
            }
            withVariantReselection()
            componentFilter { it is ProjectComponentIdentifier }
        }

        val jarArtifacts = jarArtifactsView.artifacts.resolvedArtifacts
        val dirArtifacts = sourceDirectoriesView.artifacts.resolvedArtifacts

        dependsOn(jarArtifacts, dirArtifacts)

        from(jarArtifacts.zip(dirArtifacts) { jars, dirs ->
            val projectsWithJars = jars.map { it.id.componentIdentifier }.toSet()
            val filteredDirs = dirs.filter { it.id.componentIdentifier !in projectsWithJars }

            (jars + filteredDirs)
                .sortedWith(compareBy({ it.id.componentIdentifier.toString() }, { it.file.name }))
                .map { artifact ->
                    val file = artifact.file
                    if (file.isFile && file.name.endsWith(".jar", ignoreCase = true)) {
                        archiveOperations.zipTree(file)
                    } else {
                        file
                    }
                }
        })

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}

@JvmOverloads
fun Jar.addEmbeddedJavadoc(configurationName: String = "embedded") {
    project.configurations.findByName(configurationName)?.let { embedded ->
        val archiveOperations = project.serviceOf<ArchiveOperations>()
        val objects = project.objects
        val view = embedded.incoming.artifactView {
            isLenient = true
            attributes {
                attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category::class.java, Category.DOCUMENTATION))
                attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType::class.java, DocsType.JAVADOC))
            }
            withVariantReselection()
            componentFilter { it is ProjectComponentIdentifier }
        }.artifacts.resolvedArtifacts

        dependsOn(view)
        from(view.map { artifacts ->
            artifacts.sortedBy { it.id.componentIdentifier.toString() }.map { artifact ->
                val file = artifact.file
                if (file.isFile && file.name.endsWith(".jar", ignoreCase = true)) {
                    archiveOperations.zipTree(file)
                } else {
                    file
                }
            }
        })
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}
