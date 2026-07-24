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
                ":kotlin-dataframe-compiler-plugin.k2",
                ":kotlin-dataframe-compiler-plugin.backend",
                ":kotlin-dataframe-compiler-plugin.common",
                ":kotlin-dataframe-compiler-plugin.cli",
            )
        )
    }
}
