plugins {
    id("gradle-plugins-documentation")
}

pluginsApiDocumentation {
    documentationOutput = layout.buildDirectory.dir("documentation/kotlinlang")
    documentationOldVersions = layout.buildDirectory.dir("documentation/kotlinlangOld")
    templatesArchiveUrl = "https://github.com/JetBrains/kotlin-web-site/archive/refs/heads/master.zip"
    templatesArchiveSubDirectoryPattern = "kotlin-web-site-master/dokka-templates/**"
    templatesArchivePrefixToRemove = "kotlin-web-site-master/dokka-templates/"
    gradlePluginsProjects = setOf(
        project(":kotlin-gradle-plugin-api"),
        project(":compose-compiler-gradle-plugin")
    )
}