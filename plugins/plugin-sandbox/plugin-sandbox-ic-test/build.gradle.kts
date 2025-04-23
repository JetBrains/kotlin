import org.jetbrains.kotlin.ideaExt.idea

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testApi(project(":plugins:plugin-sandbox"))
    testApi(project(":compiler:incremental-compilation-impl"))
    testApi(projectTests(":compiler:incremental-compilation-impl"))
    testImplementation(libs.junit.jupiter.api)

    testCompileOnly(intellijCore())

    testRuntimeOnly(project(":core:descriptors.runtime"))
    testRuntimeOnly(project(":compiler:fir:fir-serialization"))

    testRuntimeOnly(commonDependency("org.lz4:lz4-java"))
    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps.jna:jna"))
    testRuntimeOnly(intellijJDom())
    testRuntimeOnly(libs.intellij.fastutil)

    testRuntimeOnly(toolsJar())
    testRuntimeOnly(libs.junit.vintage.engine)
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

projectTest(parallel = true, jUnitMode = JUnitMode.JUnit4, maxHeapSizeMb = 3072) {
    dependsOn(":dist")
    workingDir = rootDir
    useJUnitPlatform()
    dependsOn(":plugins:plugin-sandbox:jar")
    dependsOn(":plugins:plugin-sandbox:plugin-annotations:distAnnotations")
}

testsJar()
