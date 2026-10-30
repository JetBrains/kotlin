plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    `java-library`
    id("analysis-api-artifact")
}

val analysisApiImplementationScriptingModules: Array<String> = CompilerModules.analysisApiImplementationScriptingModules

dependencies {
    implementation(project(":prepare:analysis-api:kotlin-analysis-api-implementation"))

    implementation(project(":kotlin-scripting-compiler")) {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
    }

    implementation(project(":kotlin-assignment-compiler-plugin")) { isTransitive = false }
}

analysisApiArtifact {
    content {
        projects(analysisApiImplementationScriptingModules.asIterable())
    }

    validateClasspath(
        proguardConfiguration = file("analysis-api-implementation-scripting.pro"),
        permittedDuplicatesFile = file("api/analysis-api-implementation-scripting.permitted-duplicates"),
    )
}
