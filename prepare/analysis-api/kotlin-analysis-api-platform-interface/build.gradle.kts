import org.jetbrains.kotlin.build.foreign.CheckForeignClassUsageTask

plugins {
    `java-library`
    id("kotlin-git.gradle-build-conventions.foreign-class-usage-checker")
}

dependencies {
    api(project(":prepare:analysis-api:kotlin-analysis-api-surface"))

    // Used by NullableCaffeineCache
    api(libs.caffeine)

    // Used by KotlinOpenTelemetryProvider
    api(libs.opentelemetry.api)

    embedded(project(":analysis:analysis-api-platform-interface")) { isTransitive = false }
}

publishAnalysisApiArtifact()

val checkForeignClassUsage by tasks.registering(CheckForeignClassUsageTask::class) {
    classes.from(tasks.jar)
    classpath.from(configurations.runtimeClasspath)
    missingClasspathEntriesOutputFile = file("api/analysis-api-platform-interface.classpath-issues")
    collectUsages = true
}