@file:Suppress("HasPlatformType")

import org.gradle.api.tasks.PathSensitivity

plugins {
    kotlin("jvm")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

dependencies {
    api(intellijCore())
    api(project(":core:compiler.common"))
    api(project(":kotlin-tooling-core"))
    api(project(":native:base"))

    testImplementation(project(":native:external-projects-test-utils"))

    if (kotlinBuildProperties.isKotlinNativeEnabled) {
        testImplementation(project(":kotlin-native:Interop:Indexer"))
        testImplementation(project(":native:kotlin-native-utils"))
        testImplementation(project(":kotlin-native:Interop:StubGenerator"))
        testImplementation(projectTests(":native:native.tests"))
    }

    testApi(libs.junit.jupiter.api)
    testApi(libs.junit.jupiter.engine)
    testApi(libs.junit.jupiter.params)
    testApi(project(":compiler:tests-common", "tests-jar"))
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.backend.konan.InternalKotlinNativeApi")
    }
}

/* Configure tests */

testsJar()

val k1TestRuntimeClasspath by configurations.creating
val analysisApiRuntimeClasspath by configurations.creating

dependencies {
    k1TestRuntimeClasspath(project(":native:objcexport-header-generator-k1"))
    k1TestRuntimeClasspath(projectTests(":native:objcexport-header-generator-k1"))

    analysisApiRuntimeClasspath(project(":native:objcexport-header-generator-analysis-api"))
    analysisApiRuntimeClasspath(projectTests(":native:objcexport-header-generator-analysis-api"))
}

tasks.test.configure {
    enabled = false
}

objCExportHeaderGeneratorTest("testK1", testDisplayNameTag = "K1") {
    classpath += k1TestRuntimeClasspath
    exclude("**/ObjCExportIntegrationTest.class")
}

objCExportHeaderGeneratorTest("testAnalysisApi", testDisplayNameTag = "AA") {
    classpath += analysisApiRuntimeClasspath
    exclude("**/ObjCExportIntegrationTest.class")
}

tasks.check.configure {
    dependsOn("testK1")
    dependsOn("testAnalysisApi")
    dependsOn("testIntegration")
    dependsOn(":native:objcexport-header-generator-k1:check")
    dependsOn(":native:objcexport-header-generator-analysis-api:check")
}

tasks.withType<Test>().configureEach {
    systemProperty(
        integrationTestOutputsDir,
        layout.buildDirectory.dir(integrationTestOutputsDir).get().asFile.absolutePath
    )
}

objCExportHeaderGeneratorTest("testIntegration", testDisplayNameTag = "testIntegration") {
    filter {
        includeTestsMatching("org.jetbrains.kotlin.backend.konan.tests.integration.ObjCExportIntegrationTest")
    }
    dependsOn("testK1", "testAnalysisApi")

    inputs.dir(
        layout.buildDirectory.dir(integrationTestOutputsDir)
    ).withPathSensitivity(
        PathSensitivity.RELATIVE
    )
}

val integrationTestOutputsDir = "integration-test-outputs"