import org.jetbrains.kotlin.*

buildscript {
    val properties = java.util.Properties()
    properties.load(java.io.FileReader(project.file("../../gradle.properties")))
    properties.forEach { (k, v) ->
        val key = k as String
        extra[key] = project.findProperty(key) ?: v
    }
}

plugins {
    id("org.jetbrains.kotlin.multiplatform") apply false
}

subprojects {
    extra["kotlin.native.home"] = kotlinNativeDist.toString()
    extra["buildNumber"]  = findProperty("build.number")?.toString() ?: property("defaultSnapshotVersion").toString()
    extra["kotlinVersion"] = findProperty("deployVersion")?.toString()?.let { deploySnapshotStr ->
                if (deploySnapshotStr != "default.snapshot") deploySnapshotStr else property("defaultSnapshotVersion").toString()
            } ?: extra["buildNumber"]

    repositories {
        mavenCentral()
    }
}

tasks.register("buildAnalyzer") {
    dependsOn(":benchmarksAnalyzer:${getHostAsTargetName()}Binaries".toString())
}

tasks.register("konanRun") {
    subprojects.forEach {
        if (it.name != "benchmarksAnalyzer" && it.name != "benchmarksLauncher") {
            dependsOn("${it.path}:konanRun")
        }
    }
}

tasks.register("clean") {
    subprojects.forEach {
        dependsOn("${it.path}:clean")
    }
    doLast {
        delete(layout.buildDirectory)
    }
}

defaultTasks("konanRun")

fun getHostAsTargetName(): String {
    val target = System.getProperty("os.name")
    if (target == "Linux") return "linuxX64"
    if (target.startsWith("Windows")) return "mingwX64"
    if (target.startsWith("Mac")) return "macosArm64"
    return "unknown"
}

fun mergeReports(fileName: String) {
    val reports = mutableListOf<File>()
    subprojects.forEach {
        val reportFile = it.layout.buildDirectory.file(fileName).get().asFile
        if (reportFile.exists()) {
            reports.add(reportFile)
        }
    }
    val output = mergeReports(reports)
    mkdir(layout.buildDirectory.get().asFile.absolutePath)
    File("${layout.buildDirectory.get().asFile.absolutePath}/${fileName}").writeText(output)
}

val mergeNativeReports by tasks.registering {
    doLast {
        mergeReports(property("nativeJson") as String)
    }
}

subprojects.forEach {
    if (it.name != "benchmarksAnalyzer" && it.name != "benchmarksLauncher") {
        it.afterEvaluate {
            it.tasks.named("konanJsonReport").configure {
                finalizedBy(mergeNativeReports)
            }
        }
    }
}

// CI calls this task to check that compilation is not broken in Aggregate
tasks.register("compileKotlinNative") {
    dependsOn(getTasksByName("compileKotlin${getHostAsTargetName().capitalize()}", true))
}

tasks.register("cinterop") {
    dependsOn("clean")
    dependsOn("cinterop:konanRun")
}

tasks.register("helloworld") {
    dependsOn("clean")
    dependsOn("helloworld:konanRun")
}

tasks.register("objcinterop") {
    dependsOn("clean")
    dependsOn("objcinterop:konanRun")
}

tasks.register("ring") {
    dependsOn("clean")
    dependsOn("ring:konanRun")
}

tasks.register("numerical") {
    dependsOn("clean")
    dependsOn("numerical:konanRun")
}

tasks.register("startup") {
    dependsOn("clean")
    dependsOn("startup:konanRun")
}

tasks.register("swiftinterop") {
    dependsOn("clean")
    dependsOn("swiftinterop:konanRun")
}
