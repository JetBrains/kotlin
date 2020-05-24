
plugins {
    kotlin("jvm")
}

jvmTarget = "1.6"

val allTestsRuntime by configurations.creating
val testCompile by configurations
testCompile.extendsFrom(allTestsRuntime)
val embeddableTestRuntime by configurations.creating {
    extendsFrom(allTestsRuntime)
}

dependencies {
    allTestsRuntime(commonDep("junit"))
    allTestsRuntime(intellijCoreDep()) { includeJars("intellij-core") }
    Platform[193].orLower {
        allTestsRuntime(intellijDep()) { includeJars("openapi") }
    }
    allTestsRuntime(intellijDep()) { includeJars("idea", "idea_rt", "log4j", "jna") }
    testCompile(project(":kotlin-scripting-jvm-host-unshaded"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(project(":kotlin-scripting-compiler-unshaded"))
    testCompile(project(":daemon-common")) // TODO: fix import (workaround for jps build)

    testRuntimeOnly(project(":kotlin-compiler"))
    testRuntimeOnly(project(":kotlin-reflect"))
    testRuntimeOnly(commonDep("org.jetbrains.intellij.deps", "trove4j"))
    
    embeddableTestRuntime(project(":kotlin-scripting-jvm-host"))
    embeddableTestRuntime(project(":kotlin-test:kotlin-test-jvm"))
    embeddableTestRuntime(project(":kotlin-test:kotlin-test-junit"))
    embeddableTestRuntime(projectTests(":compiler:tests-common")) { isTransitive = false }
    embeddableTestRuntime(testSourceSet.output)
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
    kotlinOptions.freeCompilerArgs += "-Xallow-kotlin-package"
}

projectTest(parallel = true) {
    dependsOn(":dist")
    workingDir = rootDir
}

// This doesn;t work now due to conflicts between embeddable compiler contents and intellij sdk modules
// To make it work, the dependencies to the intellij sdk should be eliminated
//projectTest(taskName = "embeddableTest", parallel = true) {
//    workingDir = rootDir
//    dependsOn(embeddableTestRuntime)
//    classpath = embeddableTestRuntime
//}
