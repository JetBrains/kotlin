description = "Lombok compiler plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    embedded(project(":kotlin-lombok-compiler-plugin.common"))
    embedded(project(":kotlin-lombok-compiler-plugin.k1"))
    embedded(project(":kotlin-lombok-compiler-plugin.k2"))
    embedded(project(":kotlin-lombok-compiler-plugin.cli"))

    testImplementation(intellijCore())
    testImplementation(project(":kotlin-lombok-compiler-plugin.common"))
    testImplementation(project(":kotlin-lombok-compiler-plugin.k1"))
    testImplementation(project(":kotlin-lombok-compiler-plugin.k2"))
    testImplementation(project(":kotlin-lombok-compiler-plugin.cli"))

    testImplementation("org.projectlombok:lombok:1.18.16")

    testApi(project(":compiler:util"))
    testApi(project(":compiler:backend"))
    testApi(project(":compiler:ir.backend.common"))
    testApi(project(":compiler:backend.jvm"))
    testApi(project(":compiler:cli"))

    testApi(projectTests(":compiler:tests-common-new"))
    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))

    // FIR dependencies
    testApi(project(":compiler:fir:checkers"))
    testApi(project(":compiler:fir:checkers:checkers.jvm"))
    testRuntimeOnly(project(":compiler:fir:fir-serialization"))

    testCompileOnly(project(":kotlin-reflect-api"))
    testRuntimeOnly(project(":kotlin-reflect"))
    testRuntimeOnly(project(":core:descriptors.runtime"))

    testApi(commonDependency("junit:junit"))


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

projectTest(parallel = true) {
    workingDir = rootDir
    dependsOn(":dist")
}

runtimeJar()
sourcesJar()
javadocJar()
testsJar()
