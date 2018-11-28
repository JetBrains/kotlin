
description = "Kotlin Android Lint"

plugins {
    `java-base`
    id("jps-compatible")
}

val projectsToShadow = listOf(
        ":plugins:lint",
        ":plugins:uast-kotlin",
        ":plugins:uast-kotlin-idea")

sourceSets {
    "main" {}
    "test" {}
}

dependencies {
    projectsToShadow.forEach { p ->
        embeddedComponents(project(p)) { isTransitive = false }
    }
}

runtimeJar {
    fromEmbeddedComponents()
}

ideaPlugin()
