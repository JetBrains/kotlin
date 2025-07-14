description = "Kotlin SamWithReceiver Compiler Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testImplementation(testFixtures(project(":kotlin-allopen-compiler-plugin")))
    testImplementation(testFixtures(project(":kotlin-assignment-compiler-plugin")))
    testImplementation(testFixtures(project(":kotlinx-serialization-compiler-plugin")))
    testImplementation(testFixtures(project(":kotlin-lombok-compiler-plugin")))
    testImplementation(testFixtures(project(":kotlin-noarg-compiler-plugin")))
    testImplementation(testFixtures(project(":plugins:parcelize:parcelize-compiler")))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
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
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

runtimeJar()
sourcesJar()
testsJar()

projectTest(jUnitMode = JUnitMode.JUnit5) {
    dependsOn(":dist")
    useJUnitPlatform()
    workingDir = rootDir
    useJUnitPlatform()
}
