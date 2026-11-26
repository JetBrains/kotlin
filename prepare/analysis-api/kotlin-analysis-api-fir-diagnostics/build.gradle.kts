import org.jetbrains.kotlin.build.foreign.registerForeignClassUsageTasks

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    `java-library`
    id("kotlin-git.gradle-build-conventions.foreign-class-usage-checker")
    id("analysis-api-artifact")
}

dependencies {
    api(project(":prepare:analysis-api:kotlin-analysis-api-surface"))
}

analysisApiArtifact {
    content {
        project(":analysis:analysis-api-fir-diagnostics")
    }

    validateClasspath(
        proguardConfiguration = file("analysis-api-fir-diagnostics.pro"),
        permittedDuplicatesFile = file("api/analysis-api-fir-diagnostics.permitted-duplicates"),
    )
}

registerForeignClassUsageTasks {
    classes.from(tasks.jar)
    classpath.from(configurations.runtimeClasspath)
    missingClasspathEntriesOutputFile = file("api/analysis-api-fir-diagnostics.classpath-issues")
}
