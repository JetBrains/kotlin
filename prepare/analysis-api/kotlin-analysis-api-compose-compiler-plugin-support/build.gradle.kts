plugins {
    id("common-configuration")
    `java-library`
    id("analysis-api-artifact")
}

dependencies {
    api(project(":prepare:analysis-api:kotlin-analysis-api-surface"))
}

analysisApiArtifact {
    content {
        project(":plugins:compose-compiler-plugin:compiler-hosted")
    }
}
