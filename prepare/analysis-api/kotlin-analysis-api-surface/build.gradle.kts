import org.jetbrains.kotlin.build.foreign.CheckForeignClassUsageTask

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    `java-library`
    id("kotlin-git.gradle-build-conventions.foreign-class-usage-checker")
    id("analysis-api-artifact")
}

val analysisApiSurfaceDependencies: List<String> = CompilerModules.analysisApiSurfaceDependencies
val analysisApiSurfaceModules: Array<String> = CompilerModules.analysisApiSurfaceModules

dependencies {
    api(project(":prepare:analysis-api:kotlin-analysis-api-intellij-api-surface-components"))
}

analysisApiArtifact {
    content {
        projects(analysisApiSurfaceDependencies)
        projects(analysisApiSurfaceModules)
    }
}

val checkForeignClassUsage = tasks.register("checkForeignClassUsage", CheckForeignClassUsageTask::class) {
    classes.from(tasks.jar)
    classpath.from(configurations.runtimeClasspath)
    missingClasspathEntriesOutputFile = file("api/analysis-api-surface.classpath-issues")
}
