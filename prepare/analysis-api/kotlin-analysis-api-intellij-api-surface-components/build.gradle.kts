plugins {
    `java-library`
}

dependencies {
    api(libs.analysis.api.kotlin.stdlib)
    embedded(project(":dependencies:intellij-java-psi-api"))
}

publishAnalysisApiArtifact()