plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    `java-library`
    id("analysis-api-artifact")
}

val analysisApiImplementationCliOptionsModules: Array<String> = CompilerModules.analysisApiImplementationCliOptionsModules

dependencies {
    api(project(":prepare:analysis-api:kotlin-analysis-api-surface"))
}

analysisApiArtifact {
    content {
        projects(analysisApiImplementationCliOptionsModules)
    }

    validateClasspath(
        proguardConfiguration = file("analysis-api-implementation-cli-options.pro"),
        permittedDuplicatesFile = file("api/analysis-api-implementation-cli-options.permitted-duplicates"),
    )
}
