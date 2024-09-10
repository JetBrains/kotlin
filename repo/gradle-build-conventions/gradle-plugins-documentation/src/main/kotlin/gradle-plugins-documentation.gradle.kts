import gradle.publishGradlePluginsJavadoc
import org.jetbrains.dokka.gradle.DokkaMultiModuleFileLayout

plugins {
    id("org.jetbrains.dokka")
    base
}

val documentationExtension = extensions.create<PluginsApiDocumentationExtension>("pluginsApiDocumentation")

dependencies {
    dokkaPlugin(versionCatalogs.named("libs").findLibrary("dokka-versioningPlugin").get())
    dokkaPlugin(versionCatalogs.named("libs").findLibrary("dokka-multiModulePlugin").get())
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
    outputDirectory.set(
        documentationExtension.documentationOutput.orElse(
            layout.buildDirectory.dir("dokka/kotlinlangDocumentation")
        )
    )

    pluginsMapConfiguration.put(
        "org.jetbrains.dokka.base.DokkaBase",
        "{ \"templatesDir\": \"${documentationExtension.templates.map { it.asFile }.get()}\" }"
    )
    pluginsMapConfiguration.put(
        "org.jetbrains.dokka.versioning.VersioningPlugin",
        documentationExtension.documentationOldVersions.map { olderVersionsDir ->
            "{ \"version\":\"$version\", \"olderVersionsDir\":\"${olderVersionsDir.asFile}\" }"
        }
    )

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
