description = "Kotlin SamWithReceiver Compiler Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("java-test-fixtures")
    id("compiler-tests-convention")
}

dependencies {
    testFixturesApi(testFixtures(project(":kotlin-allopen-compiler-plugin")))
    testFixturesApi(testFixtures(project(":kotlin-assignment-compiler-plugin")))
    testFixturesApi(testFixtures(project(":kotlinx-serialization-compiler-plugin")))
    testFixturesApi(testFixtures(project(":kotlin-lombok-compiler-plugin")))
    testFixturesApi(testFixtures(project(":kotlin-noarg-compiler-plugin")))
    testFixturesApi(testFixtures(project(":plugins:parcelize:parcelize-compiler")))

    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.vintage.engine)

    testRuntimeOnly(project(":core:descriptors.runtime"))
    testRuntimeOnly(project(":compiler:fir:fir-serialization"))

    testRuntimeOnly(commonDependency("org.codehaus.woodstox:stax2-api"))
    testRuntimeOnly(commonDependency("com.fasterxml:aalto-xml"))
    testRuntimeOnly("com.jetbrains.intellij.platform:util-xml-dom:$intellijVersion") { isTransitive = false }
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { none() }
    "test" { generatedTestDir() }
    "testFixtures" { projectDefault() }
}

runtimeJar()
sourcesJar()
testsJar()

compilerTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        dependsOn(":dist")
        workingDir = rootDir
    }
}
