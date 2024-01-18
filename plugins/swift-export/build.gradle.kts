import org.jetbrains.kotlin.kotlinNativeDist

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

    embedded(project(":analysis:analysis-api-standalone")) { isTransitive = false }
    embedded(project(":analysis:analysis-api")) { isTransitive = false }
    embedded(project(":analysis:analysis-api-fir")) { isTransitive = false }
    embedded(project(":analysis:analysis-api-impl-base")) { isTransitive = false }
    embedded(project(":analysis:analysis-api-impl-barebone")) { isTransitive = false }
    embedded(project(":analysis:analysis-api-standalone:analysis-api-standalone-base")) { isTransitive = false }
    embedded(project(":analysis:analysis-api-standalone:analysis-api-fir-standalone-base")) { isTransitive = false }
    embedded(project(":analysis:analysis-internal-utils")) { isTransitive = false }
    embedded(project(":analysis:low-level-api-fir")) { isTransitive = false }
    embedded(project(":analysis:symbol-light-classes")) { isTransitive = false }
    embedded(project(":analysis:analysis-api-providers")) { isTransitive = false }
    embedded(project(":analysis:decompiled:decompiler-to-file-stubs")) { isTransitive = false }
    embedded(project(":analysis:decompiled:decompiler-to-psi")) { isTransitive = false }
    embedded(project(":analysis:kt-references")) { isTransitive = false }
    embedded(project(":analysis:decompiled:decompiler-to-stubs")) { isTransitive = false }
    embedded(project(":analysis:decompiled:decompiler-to-file-stubs")) { isTransitive = false }

    testApi(project(":kotlin-swift-export-compiler-plugin.cli"))

    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(projectTests(":compiler:tests-common-new"))

    testImplementation(project(":kotlin-native:backend.native"))
    testRuntimeOnly(project(":kotlin-native:utilities:basic-utils"))
    testRuntimeOnly(project(":kotlin-native:Interop:Runtime"))
    testRuntimeOnly(project(":native:base"))

    testRuntimeOnly(project(":analysis:analysis-api"))
    testRuntimeOnly(project(":analysis:analysis-api-standalone"))

    testRuntimeOnly(project(":native:swift:sir"))
    testRuntimeOnly(project(":native:swift:sir-analysis-api"))
    testRuntimeOnly(project(":native:swift:sir-compiler-bridge"))
    testRuntimeOnly(project(":native:swift:sir-passes"))
    testRuntimeOnly(project(":native:swift:sir-printer"))

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

    doFirst {
        systemProperty("kotlin.native.home", kotlinNativeDist)
    }
}
