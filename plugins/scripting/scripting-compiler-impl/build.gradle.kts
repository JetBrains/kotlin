
description = "Kotlin Compiler Infrastructure for Scripting"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":compiler:psi"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":compiler:ir.serialization.web"))
    api(project(":kotlin-scripting-common"))
    api(project(":kotlin-scripting-jvm"))
    api(kotlinStdlib())
    compileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    compileOnly(intellijCore())
    compileOnly(commonDependency("org.jetbrains.intellij.deps:asm-all"))

    // FIXME: drop after removing references to LocalFileSystem they don't exist in intellij-core
    compileOnly(intellijAnalysis())

    runtimeOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }

    testApi(project(":compiler:frontend"))
    testApi(project(":compiler:plugin-api"))
    testApi(project(":compiler:util"))
    testApi(project(":compiler:cli"))
    testApi(project(":compiler:cli-common"))
    testApi(project(":compiler:frontend.java"))
    testApi(projectTests(":compiler:tests-common"))
    testApi(commonDependency("junit:junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
    kotlinOptions {
        freeCompilerArgs += "-Xskip-metadata-version-check"
    }
}

publish()

runtimeJar()
sourcesJar()
javadocJar()

projectTest {
    workingDir = rootDir
}
