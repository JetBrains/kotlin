import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    `lifecycle-base`
}

val benchmarksAnalyzer = configurations.create("benchmarksAnalyzer") {
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
val buildAnalyzer = tasks.register("buildAnalyzer", Sync::class) {
    from(benchmarksAnalyzer)
    into(layout.buildDirectory)
}

val nativeReports = configurations.create("nativeReports") {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("native-report"))
    }
}

dependencies {
    groups.forEach {
        nativeReports(project(":$it"))
    }
}

val konanRun = tasks.register("konanRun", MergeNativeReportsTask::class) {
    outputReport = layout.buildDirectory.file(nativeJson)
    inputReports.from(nativeReports)
}
