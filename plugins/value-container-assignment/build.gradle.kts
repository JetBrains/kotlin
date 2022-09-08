description = "Kotlin Value Container Assignment Compiler Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    embedded(project(":kotlin-value-container-assignment-compiler-plugin.common"))
    embedded(project(":kotlin-value-container-assignment-compiler-plugin.k1"))
    embedded(project(":kotlin-value-container-assignment-compiler-plugin.k2"))
    embedded(project(":kotlin-value-container-assignment-compiler-plugin.cli"))

    testApi(project(":compiler:backend"))
    testApi(project(":compiler:cli"))
    testApi(project(":kotlin-value-container-assignment-compiler-plugin.cli"))
    testCompileOnly(project(":kotlin-compiler"))
    testImplementation(project(":kotlin-scripting-jvm-host-unshaded"))

    testApi(projectTests(":compiler:tests-common-new"))

    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(commonDependency("junit:junit"))

    testCompileOnly(project(":kotlin-reflect-api"))
    testRuntimeOnly(project(":kotlin-reflect"))
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
