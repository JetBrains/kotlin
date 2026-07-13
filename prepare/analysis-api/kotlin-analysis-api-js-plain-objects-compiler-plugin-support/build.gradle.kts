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
                ":plugins:js-plain-objects:compiler-plugin:js-plain-objects.cli",
                ":plugins:js-plain-objects:compiler-plugin:js-plain-objects.common",
                ":plugins:js-plain-objects:compiler-plugin:js-plain-objects.k2",
                ":plugins:js-plain-objects:compiler-plugin:js-plain-objects.backend",
            )
        )
    }
}
