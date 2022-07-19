
description = "Kotlin Scripting Compiler extension providing code completion and static analysis"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

project.updateJvmTarget("1.8")

publish()

dependencies {
    api(project(":kotlin-script-runtime"))
    api(kotlinStdlib())
    compileOnly(project(":kotlin-scripting-ide-common"))
    api(project(":kotlin-scripting-common"))
    api(project(":kotlin-scripting-jvm"))
    compileOnly(project(":kotlin-scripting-compiler"))
    compileOnly(project(":compiler:cli"))
    compileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    compileOnly(intellijCore())
    publishedRuntime(project(":kotlin-compiler"))
    publishedRuntime(project(":kotlin-scripting-compiler"))
    publishedRuntime(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    publishedRuntime(commonDependency("org.jetbrains.intellij.deps", "trove4j"))
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
