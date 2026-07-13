import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

description = "Kotlin Scripting Compiler extension providing code completion and static analysis"

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
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
    compileOnly(project(":core:descriptors"))
    compileOnly(project(":compiler:container"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:resolution"))
    compileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    compileOnly(intellijCore())
    publishedRuntime(project(":kotlin-compiler"))
    publishedRuntime(project(":kotlin-scripting-compiler"))
    publishedRuntime(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
}

sourceSets {
    "main" { projectDefault() }
    "test" { }
}

optInToK1Deprecation()

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.freeCompilerArgs.addAll(
        listOf(
            "-Xskip-metadata-version-check",
            "-Xallow-kotlin-package",
        )
    )
}

standardPublicJars()
