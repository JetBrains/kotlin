plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    `java-library`
    id("analysis-api-artifact")
}

dependencies {
    api(libs.analysis.api.kotlin.stdlib)
}

analysisApiArtifact {
    content {
        project(":dependencies:intellij-java-psi-api", isTransitive = true)
    }
}
