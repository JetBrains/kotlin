import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    `lifecycle-base`
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

val nativeReports by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("native-report"))
    }
}

val benchmarkSubprojects = subprojects.filter {
    when (it.name) {
        "benchmarksAnalyzer", "benchmarksLauncher" -> false
        else -> true
    }
}

dependencies {
    groups.forEach {
        nativeReports(project(":$it"))
    }
}

val konanRun by tasks.registering(MergeNativeReportsTask::class) {
    outputReport = layout.buildDirectory.file(nativeJson)
    inputReports.from(nativeReports)
}
