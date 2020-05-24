
description = "Kotlin Scripting Compiler extension providing code completion and static analysis"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.8"

publish()

dependencies {
    compile(project(":kotlin-script-runtime"))
    compile(kotlinStdlib())
    compileOnly(project(":idea:ide-common"))
    compile(project(":kotlin-scripting-common"))
    compile(project(":kotlin-scripting-jvm"))
    compileOnly(project(":kotlin-scripting-compiler-unshaded"))
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":kotlin-reflect-api"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    publishedRuntime(project(":kotlin-compiler"))
    publishedRuntime(project(":kotlin-scripting-compiler-unshaded"))
    publishedRuntime(project(":kotlin-reflect"))
    publishedRuntime(commonDep("org.jetbrains.intellij.deps", "trove4j"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { }
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
    kotlinOptions {
        freeCompilerArgs += "-Xskip-metadata-version-check"
        freeCompilerArgs += "-Xallow-kotlin-package"
    }
}

standardPublicJars()
