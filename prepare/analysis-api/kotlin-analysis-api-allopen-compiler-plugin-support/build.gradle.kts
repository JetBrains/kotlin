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
                ":kotlin-allopen-compiler-plugin.cli",
                ":kotlin-allopen-compiler-plugin.common",
                ":kotlin-allopen-compiler-plugin.k2",
            )
        )
    }
}
