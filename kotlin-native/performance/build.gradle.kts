import org.jetbrains.kotlin.*

buildscript {
    java.util.Properties().apply {
        load(file("../../gradle.properties").inputStream())
    }.forEach { (k, v) ->
        extra[k as String] = findProperty(k) ?: v
    }
}

allprojects {
    extra["kotlin.native.home"] = kotlinNativeDist.absolutePath
    extra["buildNumber"] = findProperty("build.number") ?: property("defaultSnapshotVersion")
    extra["kotlinVersion"] = findProperty("deployVersion")?.let {
        if (it == "default.snapshot") property("defaultSnapshotVersion") else it
    } ?: extra["buildNumber"]

    repositories {
        mavenCentral()
    }
}

plugins {
    `lifecycle-base`
}

// CI calls this in Performance Tests to check that benchmarksAnalyzer successfully builds.
val buildAnalyzer by tasks.registering {
    dependsOn(":benchmarksAnalyzer:${hostKotlinNativeTarget.name}Binaries")
}

// CI calls this task to check that compilation is not broken in Aggregate
val compileKotlinNative by tasks.registering {
    dependsOn(getTasksByName("compileKotlin${hostKotlinNativeTarget.name.replaceFirstChar { it.uppercaseChar() }}", true))
}

val benchmarkSubprojects = subprojects.filter {
    it.name != "benchmarksAnalyzer" && it.name != "benchmarksLauncher"
}

val konanRun by tasks.registering {
    benchmarkSubprojects.forEach {
        dependsOn("${it.path}:konanRun")
    }
}

defaultTasks(konanRun.name)

val clean = tasks.named("clean")

clean.configure {
    benchmarkSubprojects.forEach {
        dependsOn("${it.path}:clean")
    }
}

val mergeNativeReports by tasks.registering {
    val outputFile = layout.buildDirectory.file(nativeJson)
    outputs.file(outputFile)
    doLast {
        val reports = benchmarkSubprojects.mapNotNull { p ->
            p.layout.buildDirectory.file(nativeJson).get().asFile.takeIf { it.exists() }
        }
        outputFile.get().asFile.writeText(mergeReports(reports))
    }
}

benchmarkSubprojects.forEach {
    tasks.register(it.name) {
        dependsOn(clean)
        dependsOn("${it.path}:konanRun")
    }
    it.afterEvaluate {
        tasks.named("konanJsonReport").configure {
            finalizedBy(mergeNativeReports)
        }
    }
}