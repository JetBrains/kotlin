import org.jetbrains.kotlin.build.foreign.CheckForeignClassUsageTask

plugins {
    `java-library`
    id("kotlin-git.gradle-build-conventions.foreign-class-usage-checker")
}

val analysisApiSurfaceDependencies: List<String> by rootProject.extra

val analysisApiSurfaceProjects = listOf(
    ":analysis:analysis-api",
    ":analysis:analysis-api-standalone",
)

dependencies {
    for (projectPath in analysisApiSurfaceDependencies + analysisApiSurfaceProjects) {
        embedded(project(projectPath)) { isTransitive = false }
    }

    api(project(":prepare:analysis-api:kotlin-analysis-api-intellij-api-surface-components"))
}

publishAnalysisApiArtifact()

val checkForeignClassUsage by tasks.registering(CheckForeignClassUsageTask::class) {
    classes.from(tasks.jar)
    classpath.from(configurations.runtimeClasspath)
}