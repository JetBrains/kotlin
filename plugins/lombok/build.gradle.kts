description = "Lombok compiler plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    embedded(project(":kotlin-lombok-compiler-plugin.common")) { isTransitive = false }
    embedded(project(":kotlin-lombok-compiler-plugin.k1")) { isTransitive = false }
    embedded(project(":kotlin-lombok-compiler-plugin.k2")) { isTransitive = false }
    embedded(project(":kotlin-lombok-compiler-plugin.cli")) { isTransitive = false }

    testImplementation(intellijCore())
    testApi(project(":kotlin-lombok-compiler-plugin.common"))
    testApi(project(":kotlin-lombok-compiler-plugin.k1"))
    testApi(project(":kotlin-lombok-compiler-plugin.k2"))
    testApi(project(":kotlin-lombok-compiler-plugin.cli"))

    testApi(commonDependency("org.projectlombok:lombok"))

    testApi(project(":compiler:util"))
    testApi(project(":compiler:backend"))
    testApi(project(":compiler:ir.backend.common"))
    testApi(project(":compiler:backend.jvm"))
    testApi(project(":compiler:cli"))

    testApi(projectTests(":compiler:tests-common-new"))
    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testImplementation(libs.junit.jupiter.api)

    // FIR dependencies
    testApi(project(":compiler:fir:checkers"))
    testApi(project(":compiler:fir:checkers:checkers.jvm"))
    testRuntimeOnly(project(":compiler:fir:fir-serialization"))

    testRuntimeOnly(project(":core:descriptors.runtime"))
    testRuntimeOnly(libs.junit.jupiter.engine)

    testImplementation(libs.junit4)

    testRuntimeOnly(libs.guava)
    testRuntimeOnly(commonDependency("org.codehaus.woodstox:stax2-api"))
    testRuntimeOnly(commonDependency("com.fasterxml:aalto-xml"))
    testRuntimeOnly(toolsJar())
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { none() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

projectTest(jUnitMode = JUnitMode.JUnit5) {
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
