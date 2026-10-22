plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    `java-library`
    id("analysis-api-artifact")
}

dependencies {
    api(project(":prepare:analysis-api:kotlin-analysis-api-surface"))
    implementation(project(":prepare:analysis-api:kotlin-analysis-api-implementation"))

    // The meta-artifact is designed for Standalone users, so it also bundles the Standalone artifacts
    api(project(":prepare:analysis-api:kotlin-analysis-api-standalone-surface"))
    implementation(project(":prepare:analysis-api:kotlin-analysis-api-standalone-implementation"))
}

analysisApiArtifact {
    // This umbrella artifact re-exports its dependencies and ships no binaries or sources of its own.
    expectNonEmptyBinaries = false
    expectNonEmptySources = false

    validateClasspath(
        proguardConfiguration = file("analysis-api.pro"),
        permittedDuplicatesFile = file("api/analysis-api.permitted-duplicates"),
    )
}
