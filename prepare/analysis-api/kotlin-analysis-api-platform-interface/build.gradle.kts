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

    // Used by NullableCaffeineCache
    api(libs.caffeine)

    // Used by KotlinOpenTelemetryProvider
    api(libs.opentelemetry.api)
}

analysisApiArtifact {
    content {
        project(":analysis:analysis-api-platform-interface")
    }
}

val checkForeignClassUsage = tasks.register("checkForeignClassUsage",CheckForeignClassUsageTask::class) {
    classes.from(tasks.jar)
    classpath.from(configurations.runtimeClasspath)
    missingClasspathEntriesOutputFile = file("api/analysis-api-platform-interface.classpath-issues")
    collectUsages = true
}
