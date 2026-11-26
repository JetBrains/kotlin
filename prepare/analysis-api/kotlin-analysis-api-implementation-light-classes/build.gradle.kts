plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    `java-library`
    id("analysis-api-artifact")
}

val analysisApiImplementationLightClassesModules: Array<String> = CompilerModules.analysisApiImplementationLightClassesModules

dependencies {
    api(project(":prepare:analysis-api:kotlin-analysis-api-surface"))
}

analysisApiArtifact {
    content {
        projects(analysisApiImplementationLightClassesModules)
    }

    validateClasspath(
        proguardConfiguration = file("analysis-api-implementation-light-classes.pro"),
        permittedDuplicatesFile = file("api/analysis-api-implementation-light-classes.permitted-duplicates"),
    )
}
