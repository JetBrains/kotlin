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
                ":kotlin-noarg-compiler-plugin.cli",
                ":kotlin-noarg-compiler-plugin.common",
                ":kotlin-noarg-compiler-plugin.k2",
                ":kotlin-noarg-compiler-plugin.backend",
            )
        )
    }
}
