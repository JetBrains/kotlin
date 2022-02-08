
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

publish()

dependencies {
    api(project(":kotlin-script-runtime"))
    api(kotlinStdlib())
    api(project(":kotlin-scripting-common"))
    api(project(":kotlin-scripting-jvm"))
    compileOnly(project(":kotlin-scripting-compiler"))
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":kotlin-reflect-api"))
    compileOnly(intellijCore())
    publishedRuntime(project(":kotlin-compiler"))
    publishedRuntime(project(":kotlin-scripting-compiler"))
    publishedRuntime(project(":kotlin-reflect"))
    publishedRuntime(commonDependency("org.jetbrains.intellij.deps", "trove4j"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
    kotlinOptions.freeCompilerArgs += "-Xallow-kotlin-package"
}

standardPublicJars()

