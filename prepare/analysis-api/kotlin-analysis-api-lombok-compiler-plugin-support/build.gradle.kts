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
        projects(
            listOf(
                ":kotlin-lombok-compiler-plugin.cli",
                ":kotlin-lombok-compiler-plugin.k2",
            )
        )
    }
}
