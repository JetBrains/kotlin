import org.jetbrains.gradle.benchmarks.BenchmarksExtension

buildscript {
    repositories {
        maven("https://dl.bintray.com/orangy/maven")
    }

    dependencies {
        classpath("org.jetbrains.gradle.benchmarks:benchmarks.plugin:0.1.7-dev-20")
        classpath("kotlinx.team:kotlinx.team.infra:0.1.0-dev-44")
    }
}

repositories {
    maven("https://dl.bintray.com/orangy/maven")
}

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

apply(plugin = "org.jetbrains.gradle.benchmarks.plugin")


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

    implementation("org.openjdk.jmh:jmh-core:+")
    implementation("org.jetbrains.gradle.benchmarks:runtime-jvm:0.1.7-dev-20")
}

sourceSets {
    "main" { projectDefault() }
    "test" { }
}

configure<BenchmarksExtension> {
    // Setup configurations
    configurations {
        // This one matches compilation base name, e.g. 'jvm', 'jvmTest', etc
        register("main") {
            iterations = 20
            iterationTime = 2
        }
    }
}

afterEvaluate {
    tasks.named("mainBenchmark", JavaExec::class.java) {
        dependsOn(":compiler:ir.serialization.js:fullRuntimeSources")
        workingDir(rootDir)
    }
}
