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
                ":kotlin-assignment-compiler-plugin.cli",
                ":kotlin-assignment-compiler-plugin.common",
                ":kotlin-assignment-compiler-plugin.k2",
            )
        )
    }
}
