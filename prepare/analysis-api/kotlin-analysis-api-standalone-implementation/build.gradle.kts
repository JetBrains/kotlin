plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    `java-library`
    id("analysis-api-artifact")
}

val analysisApiStandaloneSurfaceModules: Array<String> = CompilerModules.analysisApiStandaloneSurfaceModules
val analysisApiStandaloneModules: Array<String> = CompilerModules.analysisApiStandaloneModules

dependencies {
    api(project(":prepare:analysis-api:kotlin-analysis-api-standalone-surface"))
    implementation(project(":prepare:analysis-api:kotlin-analysis-api-implementation"))
}

analysisApiArtifact {
    content {
        val implementationProjects = buildSet {
            addAll(analysisApiStandaloneModules)

            // Avoid copying content of 'kotlin-analysis-api-standalone-surface'
            removeAll(analysisApiStandaloneSurfaceModules)
        }

        projects(implementationProjects)
    }
}
