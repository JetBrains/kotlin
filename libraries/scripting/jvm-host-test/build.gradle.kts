import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
    id("project-tests-convention")
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(kotlinTest("junit5"))
    testImplementation(intellijCore())
    testImplementation(project(":kotlin-scripting-jvm-host-unshaded"))
    testImplementation(testFixtures(project(":compiler:tests-compiler-utils")))
    testImplementation(project(":kotlin-scripting-compiler"))
    testImplementation(project(":daemon-common")) // TODO: fix import (workaround for jps build)

    testImplementation(libs.kotlinx.coroutines.core)

    testRuntimeOnly(project(":kotlin-compiler"))
    testImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.freeCompilerArgs.add("-Xallow-kotlin-package")
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
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

    testTask(taskName = "testWithK1", jUnitMode = JUnitMode.JUnit5, skipInLocalBuild = false) {
        dependsOn(":dist")
        workingDir = rootDir
        doFirst {
            systemProperty("kotlin.script.base.compiler.arguments", "-language-version 1.9")
        }
    }
}
