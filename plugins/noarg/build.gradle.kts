description = "Kotlin NoArg Compiler Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    embedded(project(":kotlin-noarg-compiler-plugin.common")) { isTransitive = false }
    embedded(project(":kotlin-noarg-compiler-plugin.k1")) { isTransitive = false }
    embedded(project(":kotlin-noarg-compiler-plugin.k2")) { isTransitive = false }
    embedded(project(":kotlin-noarg-compiler-plugin.backend")) { isTransitive = false }
    embedded(project(":kotlin-noarg-compiler-plugin.cli")) { isTransitive = false }

    testApi(project(":compiler:backend"))
    testApi(project(":compiler:cli"))
    testApi(project(":kotlin-noarg-compiler-plugin.cli"))

    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testApi(projectTests(":compiler:tests-common-new"))
    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))

    testRuntimeOnly(project(":core:descriptors.runtime"))
    testRuntimeOnly(project(":compiler:fir:fir-serialization"))

    testApi(intellijCore())
    testRuntimeOnly(commonDependency("org.codehaus.woodstox:stax2-api"))
    testRuntimeOnly(commonDependency("com.fasterxml:aalto-xml"))
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

publish()

runtimeJar()
sourcesJar()
javadocJar()
testsJar()

projectTest(parallel = true) {
    workingDir = rootDir
    useJUnitPlatform()
}
