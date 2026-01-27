import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    `lifecycle-base`
}

subprojects {
    // We are using bootstrap version of KGP, but we want to use a different compiler version.
    // This instructs KGP to look for the Native compiler in a given folder.
    extra["kotlin.native.home"] = kotlinNativeHome.toString()

    repositories {
        mavenCentral()
    }
}

val benchmarksAnalyzer by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("executable"))
        attribute(KotlinNativeTarget.konanTargetAttribute, hostKotlinNativeTargetName)
    }
}

dependencies {
    benchmarksAnalyzer(project(":benchmarksAnalyzer"))
}

// CI also calls this task to check that the benchmarks analyzer builds in Performance Tests
val buildAnalyzer by tasks.registering(Sync::class) {
    from(benchmarksAnalyzer)
    into(layout.buildDirectory)
}

// CI calls this task to check that compilation is not broken in Aggregate
tasks.register("compileKotlinNative") {
    dependsOn(getTasksByName("compileKotlin${hostKotlinNativeTargetName.capitalized}", true))
}

val benchmarkSubprojects = subprojects.filter {
    when (it.name) {
        "benchmarksAnalyzer", "benchmarksLauncher" -> false
        else -> true
    }
}

val clean = tasks.named("clean")

val konanRun by tasks.registering
defaultTasks(konanRun.name)

val mergeNativeReports by tasks.registering(MergeNativeReportsTask::class) {
    outputReport = layout.buildDirectory.file(nativeJson)
    benchmarkSubprojects.forEach {
        inputReports.from(it.layout.buildDirectory.file(nativeJson).get().asFile)
    }
}

konanRun.configure {
    // After :konanRun, the aggregating report must always get updated.
    finalizedBy(mergeNativeReports)
}

benchmarkSubprojects.forEach {
    konanRun.configure {
        dependsOn("${it.path}:konanJsonReport")
    }
    clean.configure {
        // Make sure all nativeReport.json from all benchmark subprojects
        dependsOn("${it.path}:clean")
    }
    it.afterEvaluate {
        it.tasks.named("konanJsonReport").configure {
            // When `:<bench-group>:konanRun` is run, the aggregating report needs to be updated (required for CI)
            finalizedBy(mergeNativeReports)
        }
    }
    tasks.register(it.name) {
        dependsOn("${it.path}:konanJsonReport")
        finalizedBy(mergeNativeReports) // when `:<bench-group>` is run, the aggregating report needs to be updated
    }
}
