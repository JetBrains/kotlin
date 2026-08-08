import org.jetbrains.kotlin.build.foreign.CheckForeignClassUsageTask

plugins {
    id("common-configuration")
    id("test-federation-convention")
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
}

// The generated diagnostics expose plenty of compiler types (`FirModuleData`, `ClassKind`, …) which are shipped in
// 'kotlin-analysis-api-implementation'. That artifact depends on this one, so the reference cannot be declared here.
val checkForeignClassUsage = tasks.register("checkForeignClassUsage", CheckForeignClassUsageTask::class) {
    classes.from(tasks.jar)
    classpath.from(configurations.runtimeClasspath)
    missingClasspathEntriesOutputFile = file("api/analysis-api-fir-diagnostics.classpath-issues")
}
