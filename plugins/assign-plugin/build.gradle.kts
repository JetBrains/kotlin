description = "Kotlin Assignment Compiler Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("java-test-fixtures")
    id("compiler-tests-convention")
}

dependencies {
    embedded(project(":kotlin-assignment-compiler-plugin.common")) { isTransitive = false }
    embedded(project(":kotlin-assignment-compiler-plugin.k1")) { isTransitive = false }
    embedded(project(":kotlin-assignment-compiler-plugin.k2")) { isTransitive = false }
    embedded(project(":kotlin-assignment-compiler-plugin.cli")) { isTransitive = false }

    testFixturesApi(project(":compiler:backend"))
    testFixturesApi(project(":compiler:cli"))
    testFixturesApi(project(":kotlin-assignment-compiler-plugin.cli"))
    testFixturesImplementation(project(":kotlin-scripting-jvm-host-unshaded"))

    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))

    testFixturesImplementation(testFixtures(project(":compiler:tests-common")))
    testFixturesImplementation(libs.junit.jupiter.api)

    testFixturesImplementation(project(":kotlin-reflect"))
    testRuntimeOnly(project(":core:descriptors.runtime"))
    testRuntimeOnly(project(":compiler:fir:fir-serialization"))
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(toolsJar())

    testFixturesApi(intellijCore())
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { none() }
    "test" {
        generatedTestDir()
    }
    "testFixtures" {
        projectDefault()
    }
}

publish()

runtimeJar()
sourcesJar()
javadocJar()
testsJar()

compilerTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        dependsOn(":dist")
        workingDir = rootDir
    }
}
