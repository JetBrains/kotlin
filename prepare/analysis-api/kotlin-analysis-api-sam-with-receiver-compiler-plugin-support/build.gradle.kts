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
                ":kotlin-sam-with-receiver-compiler-plugin.cli",
                ":kotlin-sam-with-receiver-compiler-plugin.common",
                ":kotlin-sam-with-receiver-compiler-plugin.k2",
            )
        )
    }
}
