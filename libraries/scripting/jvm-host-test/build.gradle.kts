import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
}

dependencies {
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testApi(libs.junit.platform.launcher)
    testApi(kotlinTest("junit5"))
    testApi(intellijCore())
    testApi(project(":kotlin-scripting-jvm-host-unshaded"))
    testApi(projectTests(":compiler:tests-compiler-utils"))
    testApi(project(":kotlin-scripting-compiler"))
    testApi(project(":daemon-common")) // TODO: fix import (workaround for jps build)

    testImplementation(libs.kotlinx.coroutines.core)

    testRuntimeOnly(project(":kotlin-compiler"))
    testImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps", "trove4j"))
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.freeCompilerArgs.add("-Xallow-kotlin-package")
}

projectTest(parallel = true, jUnitMode = JUnitMode.JUnit5) {
    dependsOn(":dist")
    workingDir = rootDir
    useJUnitPlatform()
}

// This doesn;t work now due to conflicts between embeddable compiler contents and intellij sdk modules
// To make it work, the dependencies to the intellij sdk should be eliminated
//projectTest(taskName = "embeddableTest", parallel = true) {
//    workingDir = rootDir
//    dependsOn(embeddableTestRuntime)
//    classpath = embeddableTestRuntime
//}

projectTest(taskName = "testWithK1", parallel = true, jUnitMode = JUnitMode.JUnit5) {
    dependsOn(":dist")
    workingDir = rootDir
    useJUnitPlatform()
    doFirst {
        systemProperty("kotlin.script.base.compiler.arguments", "-language-version 1.9")
    }
}
