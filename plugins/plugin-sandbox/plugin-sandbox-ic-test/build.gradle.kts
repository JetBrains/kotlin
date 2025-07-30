plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("java-test-fixtures")
}

dependencies {
    testFixturesApi(project(":plugins:plugin-sandbox"))
    testFixturesApi(project(":compiler:incremental-compilation-impl"))
    testFixturesApi(testFixtures(project(":compiler:incremental-compilation-impl")))
    testFixturesApi(libs.junit.jupiter.api)

    testCompileOnly(intellijCore())

    testRuntimeOnly(project(":core:descriptors.runtime"))
    testRuntimeOnly(project(":compiler:fir:fir-serialization"))
    testRuntimeOnly(project(":compiler:fir:plugin-utils"))

    testRuntimeOnly(commonDependency("org.lz4:lz4-java"))
    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps.jna:jna"))
    testRuntimeOnly(intellijJDom())
    testRuntimeOnly(libs.intellij.fastutil)

    testRuntimeOnly(toolsJar())
    testRuntimeOnly(libs.junit.vintage.engine)
}

sourceSets {
    "main" { none() }
    "test" { generatedTestDir() }
    "testFixtures" { projectDefault() }
}

projectTest(parallel = true, jUnitMode = JUnitMode.JUnit4, maxHeapSizeMb = 3072) {
    dependsOn(":dist")
    workingDir = rootDir
    useJUnitPlatform()
    dependsOn(":plugins:plugin-sandbox:jar")
    dependsOn(":plugins:plugin-sandbox:plugin-annotations:distAnnotations")
}

testsJar()
