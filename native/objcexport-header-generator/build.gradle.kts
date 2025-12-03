@file:Suppress("HasPlatformType")

plugins {
    kotlin("jvm")
    id("project-tests-convention")
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

    if (kotlinBuildProperties.isKotlinNativeEnabled) {
        testImplementation(project(":kotlin-native:Interop:Indexer"))
        testImplementation(project(":native:kotlin-native-utils"))
        testImplementation(project(":kotlin-native:Interop:StubGenerator"))
        testImplementation(testFixtures(project(":native:native.tests")))
    }

    testImplementation(project(":native:external-projects-test-utils"))
    testRuntimeOnly(project(":native:analysis-api-based-test-utils"))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(testFixtures(project(":compiler:tests-common")))
    api(project(":kotlin-stdlib"))
    testImplementation(project(":kotlin-stdlib"))
    testImplementation(project(":kotlin-test"))
    testImplementation(testFixtures(project(":compiler:test-infrastructure-utils")))
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

projectTests {
    objCExportHeaderGeneratorTestTask("testK1", testDisplayNameTag = "K1") {
        classpath += k1TestRuntimeClasspath
        exclude("**/ObjCExportIntegrationTest.class")
    }

    objCExportHeaderGeneratorTestTask("testAnalysisApi", testDisplayNameTag = "AA") {
        classpath += analysisApiRuntimeClasspath
        exclude("**/ObjCExportIntegrationTest.class")
    }
}

projectTests {
    objCExportHeaderGeneratorTestTask("testIntegration") {
        classpath += k1TestRuntimeClasspath
        classpath += analysisApiRuntimeClasspath
        include("**/ObjCExportIntegrationTest.class")
    }
}

tasks.check.configure {
    dependsOn("testK1")
    dependsOn("testAnalysisApi")
    dependsOn("testIntegration")
    dependsOn(":native:objcexport-header-generator-k1:check")
    dependsOn(":native:objcexport-header-generator-analysis-api:check")
}