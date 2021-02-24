
description = "Kotlin SamWithReceiver IDEA Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":kotlin-sam-with-receiver-compiler-plugin"))

    compileOnly(project(":plugins:annotation-based-compiler-plugins-ide-support"))
    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":idea:idea-core"))

    compileOnly(project(":idea"))
    compileOnly(project(":idea:idea-jvm"))

    Platform[193].orLower {
        compileOnly(intellijDep()) { includeJars("openapi") }
    }
    compileOnly(intellijDep()) { includeJars("platform-api", "extensions", "util") }
    compileOnly(intellijDep("gradle"))
    compileOnly(project(":idea:kotlin-gradle-tooling"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

runtimeJar()

sourcesJar()

javadocJar()

apply(from = "$rootDir/gradle/kotlinPluginPublication.gradle.kts")
