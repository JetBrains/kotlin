
plugins {
    kotlin("jvm")
}

val allTestsRuntime by configurations.creating
val testApi by configurations
testApi.extendsFrom(allTestsRuntime)
val embeddableTestRuntime by configurations.creating {
    extendsFrom(allTestsRuntime)
}

dependencies {
    allTestsRuntime(commonDep("junit"))
    allTestsRuntime(intellijCoreDep()) { includeJars("intellij-core") }
    allTestsRuntime(intellijDep()) { includeJars("idea", "idea_rt", "log4j", "jna") }
    testApi(project(":kotlin-scripting-jvm-host-unshaded"))
    testApi(projectTests(":compiler:tests-common"))
    testApi(project(":kotlin-scripting-compiler"))
    testApi(project(":daemon-common")) // TODO: fix import (workaround for jps build)
    testImplementation(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-core"))

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

projectTest(taskName = "testWithIr", parallel = true) {
    dependsOn(":dist")
    workingDir = rootDir
    systemProperty("kotlin.script.base.compiler.arguments", "-Xuse-ir")
}
