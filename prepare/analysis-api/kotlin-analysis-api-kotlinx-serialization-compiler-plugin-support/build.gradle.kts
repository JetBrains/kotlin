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
                ":kotlinx-serialization-compiler-plugin.cli",
                ":kotlinx-serialization-compiler-plugin.common",
                ":kotlinx-serialization-compiler-plugin.k2",
                ":kotlinx-serialization-compiler-plugin.backend",
            )
        )
    }
}
