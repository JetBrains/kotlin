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

val konanRun by tasks.registering
defaultTasks(konanRun.name)

val mergeNativeReports by tasks.registering(MergeNativeReportsTask::class) {
    outputReport = layout.buildDirectory.file(nativeJson)
    inputReports.from(nativeReports)
}

benchmarkSubprojects.forEach {
    val benchmarkGroupRun = tasks.register(it.name) {
        // When :<bench-group> is requested either via CLI or by :konanRun, add a new
        // dependency into `nativeReports` to be processed by `mergeNativeReports`.
        dependencies {
            nativeReports(project(it.path))
        }
        finalizedBy(mergeNativeReports) // when `:<bench-group>` is run, the aggregating report needs to be updated
    }
    konanRun.configure {
        dependsOn(benchmarkGroupRun)
    }
}
