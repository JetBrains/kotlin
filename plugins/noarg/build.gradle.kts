description = "Kotlin NoArg Compiler Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    embedded(project(":kotlin-noarg-compiler-plugin.common"))
    embedded(project(":kotlin-noarg-compiler-plugin.k1"))
    embedded(project(":kotlin-noarg-compiler-plugin.k2"))
    embedded(project(":kotlin-noarg-compiler-plugin.backend"))
    embedded(project(":kotlin-noarg-compiler-plugin.cli"))

    testApi(project(":compiler:backend"))
    testApi(project(":compiler:cli"))
    testApi(project(":kotlin-noarg-compiler-plugin.cli"))

    testApiJUnit5()
    testApi(projectTests(":compiler:tests-common-new"))
    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))

    testRuntimeOnly(project(":core:descriptors.runtime"))
    testRuntimeOnly(project(":compiler:fir:fir-serialization"))

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

optInToExperimentalCompilerApi()

runtimeJar()
sourcesJar()
javadocJar()
testsJar()

projectTest(parallel = true) {
    workingDir = rootDir
    useJUnitPlatform()
}
