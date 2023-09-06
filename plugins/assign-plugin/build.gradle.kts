description = "Kotlin Assignment Compiler Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    embedded(project(":kotlin-assignment-compiler-plugin.common")) { isTransitive = false }
    embedded(project(":kotlin-assignment-compiler-plugin.k1")) { isTransitive = false }
    embedded(project(":kotlin-assignment-compiler-plugin.k2")) { isTransitive = false }
    embedded(project(":kotlin-assignment-compiler-plugin.cli")) { isTransitive = false }

    testApi(project(":compiler:backend"))
    testApi(project(":compiler:cli"))
    testApi(project(":kotlin-assignment-compiler-plugin.cli"))
    testCompileOnly(project(":kotlin-compiler"))
    testImplementation(project(":kotlin-scripting-jvm-host-unshaded"))

    testApi(projectTests(":compiler:tests-common-new"))

    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(libs.junit.jupiter.api)

    testCompileOnly(project(":kotlin-reflect-api"))
    testRuntimeOnly(project(":kotlin-reflect"))
    testRuntimeOnly(project(":core:descriptors.runtime"))
    testRuntimeOnly(project(":compiler:fir:fir-serialization"))
    testRuntimeOnly(libs.junit.jupiter.engine)

    testApi(intellijCore())
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { none() }
    "test" {
        projectDefault()
        generatedTestDir()
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
