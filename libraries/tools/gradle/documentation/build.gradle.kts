plugins {
    id("gradle-plugins-documentation")
}

pluginsApiDocumentation {
    documentationOutput = layout.buildDirectory.dir("documentation/kotlinlang")
    gradlePluginsProjects = setOf(project(":kotlin-gradle-plugin-api"))
}