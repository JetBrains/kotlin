import de.undercouch.gradle.tasks.download.Download
import gradle.publishGradlePluginsJavadoc
import org.jetbrains.dokka.gradle.DokkaMultiModuleFileLayout
import org.jetbrains.dokka.gradle.DokkaTaskPartial

plugins {
    id("org.jetbrains.dokka")
    base
}

private val templateConfig = Pair(
    "org.jetbrains.dokka.base.DokkaBase",
    "{ \"templatesDir\": \"${project.rootDir.resolve("build/api-reference/templates").also { it.mkdirs() }}\" }"
)

// Documentation: https://github.com/Kotlin/dokka/tree/1.9.20/dokka-subprojects/plugin-versioning
private val PluginsApiDocumentationExtension.versioningConfig
    get() = Pair(
        "org.jetbrains.dokka.versioning.VersioningPlugin",
        documentationOldVersions.map { olderVersionsDir ->
            "{ \"version\":\"$version\", \"olderVersionsDir\":\"${olderVersionsDir.asFile.also { it.mkdirs() }}\" }"
        }
    )

private val dokkaVersioningPlugin = versionCatalogs.named("libs").findLibrary("dokka-versioningPlugin").get()

val documentationExtension = extensions.create<PluginsApiDocumentationExtension>(
    "pluginsApiDocumentation",
    { project: Project, extension: PluginsApiDocumentationExtension ->
        project.plugins.withId("org.jetbrains.dokka") {
            project.dependencies {
                "dokkaPlugin"(dokkaVersioningPlugin)
            }
            project.tasks.withType<DokkaTaskPartial>().configureEach {
                pluginsMapConfiguration.put(templateConfig.first, templateConfig.second)
                extension.versioningConfig.let { pluginsMapConfiguration.put(it.first, it.second) }
            }
        }
    }
)

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

tasks.register<org.jetbrains.dokka.gradle.DokkaMultiModuleTask>("dokkaKotlinlangDocumentation") {
    description = "Generates documentation for Kotlin Gradle plugins API reference"
    group = "Documentation"
    onlyIf("Dokka tasks only enabled on TeamCity (-Pteamcity=true)") {
        // Dokka v1 Gradle plugin is not compatible with newer Gradle versions
        // remove this once Dokka v2 Gradle plugin will be used
        kotlinBuildProperties.publishGradlePluginsJavadoc
    }

    // https://github.com/Kotlin/dokka/issues/1217
    notCompatibleWithConfigurationCache("Dokka is not compatible with Gradle Configuration Cache")

    moduleName.set("Kotlin Gradle Plugins API reference")
    includes.from(documentationExtension.moduleDescription)
    outputDirectory.set(
        documentationExtension.documentationOutput.orElse(
            layout.buildDirectory.dir("dokka/kotlinlangDocumentation")
        )
    )

    dependsOn(unzipTemplates)
    pluginsMapConfiguration.put(templateConfig.first, templateConfig.second)
    documentationExtension.versioningConfig.let {
        pluginsMapConfiguration.put(it.first, it.second)
    }

    fileLayout.set(DokkaMultiModuleFileLayout { parent, child ->
        parent.outputDirectory.dir(child.project.name)
    })

    @Suppress("DEPRECATION")
    addChildTasks(
        documentationExtension
            .gradlePluginsProjects
            .getOrElse(emptySet<Project>()),
        "dokkaHtmlPartial"
    )
}
