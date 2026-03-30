plugins {
    `java-library`
    id("analysis-api-artifact")
}

dependencies {
    api(libs.analysis.api.kotlin.stdlib)
    embedded(project(":dependencies:intellij-java-psi-api"))
}
