plugins {
    id("gradle-plugins-documentation")
}

pluginsApiDocumentation {
    documentationOutput = layout.buildDirectory.dir("documentation/kotlinlang")
    documentationOldVersions = layout.buildDirectory.dir("documentation/kotlinlangOld")
    gradlePluginsProjects = setOf(project(":kotlin-gradle-plugin-api"))
}