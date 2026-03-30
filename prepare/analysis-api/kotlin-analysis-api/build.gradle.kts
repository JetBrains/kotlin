plugins {
    `java-library`
}

dependencies {
    api(project(":prepare:analysis-api:kotlin-analysis-api-surface"))
    implementation(project(":prepare:analysis-api:kotlin-analysis-api-implementation"))
}

publishAnalysisApiArtifact()