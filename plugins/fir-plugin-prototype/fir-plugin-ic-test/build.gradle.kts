import org.jetbrains.kotlin.ideaExt.idea

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testApi(project(":plugins:fir-plugin-prototype"))
    testApi(project(":compiler:incremental-compilation-impl"))
    testApi(projectTests(":compiler:incremental-compilation-impl"))

    testCompileOnly(intellijCore())
    testCompileOnly(project(":kotlin-reflect-api"))

    testRuntimeOnly(project(":kotlin-reflect"))
    testRuntimeOnly(project(":core:descriptors.runtime"))
    testRuntimeOnly(project(":compiler:fir:fir-serialization"))

    testRuntimeOnly(commonDependency("org.lz4:lz4-java"))
    testRuntimeOnly(commonDependency("net.java.dev.jna:jna"))
    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps:jdom"))
    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps:trove4j"))
    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps.fastutil:intellij-deps-fastutil"))

    testRuntimeOnly(toolsJar())
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
    workingDir = rootDir
    useJUnitPlatform()
    dependsOn(":plugins:fir-plugin-prototype:jar")
    dependsOn(":plugins:fir-plugin-prototype:plugin-annotations:jar")
}

testsJar()
