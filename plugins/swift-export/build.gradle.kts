description = "Swift Export Compiler Plugin"

plugins {
    kotlin("jvm")
}

dependencies {
    embedded(project(":kotlin-swift-export-compiler-plugin.backend")) { isTransitive = false }
    embedded(project(":kotlin-swift-export-compiler-plugin.cli")) { isTransitive = false }

    embedded(project(":native:swift:sir")) { isTransitive = false }
    embedded(project(":native:swift:sir-analysis-api")) { isTransitive = false }
    embedded(project(":native:swift:sir-compiler-bridge")) { isTransitive = false }
    embedded(project(":native:swift:sir-passes")) { isTransitive = false }
    embedded(project(":native:swift:sir-printer")) { isTransitive = false }

    testApi(project(":kotlin-swift-export-compiler-plugin.cli"))

    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(projectTests(":compiler:tests-common-new"))


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

if (project.kotlinBuildProperties.isSwiftExportPluginPublishingEnabled) {
    // todo: is you are removing this check - don't forget to run tests in repo/artifacts-tests/src/test/kotlin/org/jetbrains/kotlin/code/ArtifactsTest.kt
    publish()
}

runtimeJar()
sourcesJar()
javadocJar()
testsJar()

val testDataDir = projectDir.resolve("testData")

projectTest(parallel = true, jUnitMode = JUnitMode.JUnit5) {
    workingDir = rootDir
    dependsOn(":dist")
    inputs.dir(testDataDir)
    useJUnitPlatform()
}
