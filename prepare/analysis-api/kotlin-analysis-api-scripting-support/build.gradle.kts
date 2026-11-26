plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    `java-library`
    id("analysis-api-artifact")
}

val analysisApiScriptingSupportModules: Array<String> = CompilerModules.analysisApiScriptingSupportModules

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
        projects(analysisApiScriptingSupportModules.asIterable())
    }

    validateClasspath(
        proguardConfiguration = file("analysis-api-scripting-support.pro"),
        permittedDuplicatesFile = file("api/analysis-api-scripting-support.permitted-duplicates"),
    )
}
