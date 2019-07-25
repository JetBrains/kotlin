import kotlinx.benchmark.gradle.JvmBenchmarkTarget
import kotlinx.benchmark.gradle.benchmark

buildscript {
    repositories {
        maven("https://dl.bintray.com/orangy/maven")
        maven("https://dl.bintray.com/kotlin/kotlinx")
    }

    dependencies {
        classpath("org.jetbrains.kotlinx:kotlinx.benchmark.gradle:0.2.0-dev-2")
        classpath("kotlinx.team:kotlinx.team.infra:0.1.0-dev-49")
    }
}

repositories {
    maven("https://dl.bintray.com/orangy/maven")
    maven("https://dl.bintray.com/kotlin/kotlinx")
}

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

apply(plugin = "kotlinx.benchmark")


dependencies {
    runtime(intellijDep())

    compileOnly(project(":compiler:frontend"))
    compile(project(":compiler:cli"))
    compileOnly(project(":compiler:util"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compile(project(":compiler:backend.js"))
    compile(project(":compiler:ir.serialization.js"))
    compile(project(":kotlin-build-common"))
    runtime(project(":compiler:backend-common"))
    runtime(project(":kotlin-reflect"))

//    implementation("org.openjdk.jmh:jmh-core:+")
    implementation("org.jetbrains.kotlinx:kotlinx.benchmark.runtime:+")
}

sourceSets {
    "main" { projectDefault() }
    "test" { }
}

benchmark {

    targets {
        register("main") {
            (this as JvmBenchmarkTarget).jmhVersion = "1.21"
        }
    }
}



//configure<BenchmarksExtension> {
//    // Setup configurations
//    configurations {
//        // This one matches compilation base name, e.g. 'jvm', 'jvmTest', etc
//        register("main") {
//            iterations = 20
//            iterationTime = 2
//        }
//    }
//}


afterEvaluate {
//    tasks.named("mainBenchmark", JavaExec::class.java) {
//        dependsOn(":compiler:ir.serialization.js:fullRuntimeSources")
//        workingDir(rootDir)
//    }

    tasks.named("mainBenchmarkJar", org.gradle.jvm.tasks.Jar::class.java) {
        isZip64 = true
    }

}
