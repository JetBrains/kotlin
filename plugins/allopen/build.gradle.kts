description = "Kotlin AllOpen Compiler Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    embedded(project(":kotlin-allopen-compiler-plugin.common")) { isTransitive = false }
    embedded(project(":kotlin-allopen-compiler-plugin.cli")) { isTransitive = false }
    embedded(project(":kotlin-allopen-compiler-plugin.k1")) { isTransitive = false }
    embedded(project(":kotlin-allopen-compiler-plugin.k2")) { isTransitive = false }

    testImplementation(project(":kotlin-allopen-compiler-plugin"))
    testImplementation(project(":kotlin-allopen-compiler-plugin.common"))
    testImplementation(project(":kotlin-allopen-compiler-plugin.k1"))
    testImplementation(project(":kotlin-allopen-compiler-plugin.k2"))
    testImplementation(project(":kotlin-allopen-compiler-plugin.cli"))
    testImplementation(project(":compiler:backend"))
    testImplementation(project(":compiler:cli"))

    testImplementation(intellijCore())
    testRuntimeOnly(commonDependency("org.codehaus.woodstox:stax2-api"))
    testRuntimeOnly(commonDependency("com.fasterxml:aalto-xml"))

    testApiJUnit5()
    testImplementation(projectTests(":compiler:tests-common-new"))
    testImplementation(projectTests(":compiler:test-infrastructure"))
    testImplementation(projectTests(":compiler:test-infrastructure-utils"))
    testImplementation(project(":compiler:fir:checkers"))
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
