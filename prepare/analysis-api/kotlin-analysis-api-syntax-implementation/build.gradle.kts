plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    `java-library`
    id("analysis-api-artifact")
}

val analysisApiSyntaxImplementationModules: Array<String> = CompilerModules.analysisApiSyntaxImplementationModules

dependencies {
    api(project(":prepare:analysis-api:kotlin-analysis-api-syntax-surface"))

    implementation(project(":prepare:analysis-api:kotlin-analysis-api-intellij-implementation-components"))
}

analysisApiArtifact {
    content {
        projects(analysisApiSyntaxImplementationModules)
    }

    validateClasspath(
        proguardConfiguration = file("analysis-api-syntax-implementation.pro"),
        permittedDuplicatesFile = file("api/analysis-api-syntax-implementation.permitted-duplicates"),
    )
}
