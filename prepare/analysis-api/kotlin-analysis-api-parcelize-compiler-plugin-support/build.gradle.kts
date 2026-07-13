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
                ":plugins:parcelize:parcelize-compiler:parcelize.backend",
                ":plugins:parcelize:parcelize-compiler:parcelize.cli",
                ":plugins:parcelize:parcelize-compiler:parcelize.common",
                ":plugins:parcelize:parcelize-compiler:parcelize.k2",
                ":plugins:parcelize:parcelize-runtime",
            )
        )
    }
}
