import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

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
    compileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    compileOnly(intellijCore())
    publishedRuntime(project(":kotlin-compiler"))
    publishedRuntime(project(":kotlin-scripting-compiler"))
    publishedRuntime(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    publishedRuntime(commonDependency("org.jetbrains.intellij.deps", "trove4j"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.freeCompilerArgs.add("-Xallow-kotlin-package")
}

standardPublicJars()

