plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

val artifactContent = configurations.dependencyScope("artifactContent") {
    // Do not include stdlib as an embedded artifact.
    exclude("org.jetbrains.kotlin", "kotlin-stdlib")
}

configurations.embedded {
    extendsFrom(artifactContent)
}

dependencies {
    // Add stdlib as a transitive dependency.
    runtimeOnly(kotlinStdlib())

    artifactContent(project(":compiler:fir:plugin.api"))
    artifactContent(project(":compiler:ir.backend.common"))
    artifactContent(project(":compiler:plugin-api"))
}

runtimeJar()
sourcesJar()
javadocJar()

publish()
