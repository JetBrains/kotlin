import de.undercouch.gradle.tasks.download.Download
import gradle.publishGradlePluginsJavadoc
import org.jetbrains.dokka.gradle.tasks.DokkaBaseTask

plugins {
    id("org.jetbrains.dokka")
    base
}

private val dokkaVersioningPlugin = versionCatalogs.named("libs").findLibrary("dokka-versioningPlugin").get()
val documentationExtension = extensions.create<PluginsApiDocumentationExtension>("pluginsApiDocumentation")

dependencies {
    dokkaPlugin(dokkaVersioningPlugin)
    dokkaPlugin(versionCatalogs.named("libs").findLibrary("dokka-multiModulePlugin").get())
}

val downloadTask = tasks.register<Download>("downloadTemplates") {
    src(documentationExtension.templatesArchiveUrl)
    dest(layout.buildDirectory.file("templateDist.zip"))
    onlyIf(
        "Kotlinlang Dokka template is not working in the standalone mode: KT-73082"
    ) {
        false
    }

    onlyIfModified(true)
    overwrite(false)
}

val unzipTemplates = tasks.register<Copy>("unzipTemplates") {
    dependsOn(downloadTask)
    onlyIf(
        "Kotlinlang Dokka template is not working in the standalone mode: KT-73082"
    ) {
        false
    }

    val dirPrefix = documentationExtension.templatesArchivePrefixToRemove
    from(
        zipTree(downloadTask.map { it.dest })
            .matching {
                include(documentationExtension.templatesArchiveSubDirectoryPattern.get())
            }
    ).eachFile {
        path = path.removePrefix(dirPrefix.get())
    }
    into(layout.buildDirectory.dir("template"))
}

dokka {
    description = "Generates documentation for Kotlin Gradle plugins API reference"
    moduleName.set("Kotlin Gradle Plugins API reference")
    dokkaSourceSets.configureEach {
        includes.from(documentationExtension.moduleDescription)
    }

    dokkaPublications.html {
        outputDirectory.set(
            documentationExtension.documentationOutput.orElse(
                layout.buildDirectory.dir("dokka/kotlinlangDocumentation")
            )
        )

    }

    pluginsConfiguration.html {
        templatesDir.set(project.rootDir.resolve("build/api-reference/templates").also { it.mkdirs() })
    }

    documentationExtension.documentationOldVersions.map { oldVersion ->
        pluginsConfiguration.versioning {
            version.set(project.version.toString())
            olderVersionsDir.set(oldVersion.asFile.also { it.mkdirs() })
        }
    }


}

tasks.withType<DokkaBaseTask>().configureEach {
    val publishGradlePluginsJavadoc = kotlinBuildProperties.publishGradlePluginsJavadoc
    onlyIf("Dokka tasks only enabled on TeamCity (-Pteamcity=true)") {
        // Dokka v1 Gradle plugin is not compatible with newer Gradle versions
        // remove this once Dokka v2 Gradle plugin will be used
        publishGradlePluginsJavadoc
    }
    dependsOn(unzipTemplates)
}
