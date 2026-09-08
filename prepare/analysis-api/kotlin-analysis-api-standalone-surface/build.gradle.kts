plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    `java-library`
    id("analysis-api-artifact")
}

val analysisApiStandaloneSurfaceModules: Array<String> = CompilerModules.analysisApiStandaloneSurfaceModules

dependencies {
    api(project(":prepare:analysis-api:kotlin-analysis-api-surface"))
}

analysisApiArtifact {
    content {
        projects(analysisApiStandaloneSurfaceModules)
    }
}
