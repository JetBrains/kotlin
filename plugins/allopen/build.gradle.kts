description = "Kotlin AllOpen Compiler Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("java-test-fixtures")
}

dependencies {
    embedded(project(":kotlin-allopen-compiler-plugin.common")) { isTransitive = false }
    embedded(project(":kotlin-allopen-compiler-plugin.cli")) { isTransitive = false }
    embedded(project(":kotlin-allopen-compiler-plugin.k1")) { isTransitive = false }
    embedded(project(":kotlin-allopen-compiler-plugin.k2")) { isTransitive = false }

    testFixturesImplementation(project(":kotlin-allopen-compiler-plugin"))
    testFixturesImplementation(project(":kotlin-allopen-compiler-plugin.common"))
    testFixturesImplementation(project(":kotlin-allopen-compiler-plugin.k1"))
    testFixturesImplementation(project(":kotlin-allopen-compiler-plugin.k2"))
    testFixturesImplementation(project(":kotlin-allopen-compiler-plugin.cli"))
    testFixturesImplementation(project(":compiler:backend"))
    testFixturesImplementation(project(":compiler:cli"))

    testFixturesImplementation(intellijCore())
    testRuntimeOnly(commonDependency("org.codehaus.woodstox:stax2-api"))
    testRuntimeOnly(commonDependency("com.fasterxml:aalto-xml"))

    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure")))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure-utils")))
    testFixturesImplementation(project(":compiler:fir:checkers"))
    testRuntimeOnly(project(":compiler:fir:fir-serialization"))

    testRuntimeOnly(project(":core:descriptors.runtime"))
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { none() }
    "test" {
        projectDefault()
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

projectTest(parallel = true) {
    dependsOn(":dist")
    workingDir = rootDir
    useJUnitPlatform()
}
