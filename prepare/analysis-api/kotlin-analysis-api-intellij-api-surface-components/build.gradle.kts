plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    `java-library`
    id("analysis-api-artifact")
}

dependencies {
    api(libs.analysis.api.kotlin.stdlib)
}

analysisApiArtifact {
    content {
        project(":dependencies:intellij-java-psi-api")
    }
}
