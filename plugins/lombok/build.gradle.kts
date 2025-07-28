description = "Lombok compiler plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("java-test-fixtures")
}

dependencies {
    embedded(project(":kotlin-lombok-compiler-plugin.common")) { isTransitive = false }
    embedded(project(":kotlin-lombok-compiler-plugin.k1")) { isTransitive = false }
    embedded(project(":kotlin-lombok-compiler-plugin.k2")) { isTransitive = false }
    embedded(project(":kotlin-lombok-compiler-plugin.cli")) { isTransitive = false }

    testFixturesApi(intellijCore())
    testFixturesApi(project(":kotlin-lombok-compiler-plugin.common"))
    testFixturesApi(project(":kotlin-lombok-compiler-plugin.k1"))
    testFixturesApi(project(":kotlin-lombok-compiler-plugin.k2"))
    testFixturesApi(project(":kotlin-lombok-compiler-plugin.cli"))

    testFixturesApi(commonDependency("org.projectlombok:lombok"))

    testFixturesApi(project(":compiler:util"))
    testFixturesApi(project(":compiler:backend"))
    testFixturesApi(project(":compiler:ir.backend.common"))
    testFixturesApi(project(":compiler:backend.jvm"))
    testFixturesApi(project(":compiler:cli"))

    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure")))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure-utils")))
    testFixturesApi(libs.junit.jupiter.api)

    // FIR dependencies
    testFixturesApi(project(":compiler:fir:checkers"))
    testFixturesApi(project(":compiler:fir:checkers:checkers.jvm"))
    testRuntimeOnly(project(":compiler:fir:fir-serialization"))

    testRuntimeOnly(project(":core:descriptors.runtime"))
    testRuntimeOnly(libs.junit.jupiter.engine)

    testFixturesApi(libs.junit4)

    testRuntimeOnly(libs.guava)
    testRuntimeOnly(commonDependency("org.codehaus.woodstox:stax2-api"))
    testRuntimeOnly(commonDependency("com.fasterxml:aalto-xml"))
    testRuntimeOnly(toolsJar())
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { none() }
    "test" { generatedTestDir() }
    "testFixtures" { projectDefault() }
}

projectTest(jUnitMode = JUnitMode.JUnit5) {
    dependsOn(":dist")
    useJUnitPlatform()
    workingDir = rootDir

    val testRuntimeClasspathFiles: FileCollection = configurations.testRuntimeClasspath.get()
    doFirst {
        testRuntimeClasspathFiles
            .find { "guava" in it.name }
            ?.absolutePath
            ?.let { systemProperty("org.jetbrains.kotlin.test.guava-location", it) }

    }
}

publish()

runtimeJar()
sourcesJar()
javadocJar()
testsJar()
