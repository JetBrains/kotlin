@file:Suppress("HasPlatformType")

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("project-tests-convention")
    id("java-test-fixtures")
    id("test-inputs-check-v2")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

dependencies {
    api(intellijCore())
    api(project(":core:compiler.common"))
    api(project(":kotlin-tooling-core"))
    api(project(":native:base"))

    if (kotlinBuildProperties.isKotlinNativeEnabled.get()) {
        testImplementation(project(":kotlin-native:Interop:Indexer"))
        testImplementation(project(":native:kotlin-native-utils"))
        testImplementation(project(":kotlin-native:Interop:StubGenerator"))
        testImplementation(project(":native:unsafe-mem"))
        testImplementation(testFixtures(project(":native:native.tests")))
    }

    testFixturesApi(project(":native:external-projects-test-utils"))
    testRuntimeOnly(project(":native:analysis-api-based-test-utils"))
    testFixturesImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
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

val k1TestRuntimeClasspath = configurations.create("k1TestRuntimeClasspath")
val analysisApiRuntimeClasspath = configurations.create("analysisApiRuntimeClasspath")

dependencies {
    k1TestRuntimeClasspath(project(":native:objcexport-header-generator-k1"))
    k1TestRuntimeClasspath(testFixtures(project(":native:objcexport-header-generator-k1")))

    analysisApiRuntimeClasspath(project(":native:objcexport-header-generator-analysis-api"))
    analysisApiRuntimeClasspath(testFixtures(project(":native:objcexport-header-generator-analysis-api")))
}

tasks.test.configure {
    enabled = false
}

projectTests {
    testData(isolated, "testData")

    objCExportHeaderGeneratorTestTask("testK1", testDisplayNameTag = "K1") {
        classpath += k1TestRuntimeClasspath
        exclude("**/ObjCExportIntegrationTest.class")
    }

    objCExportHeaderGeneratorTestTask(
        "testAnalysisApi",
        testDisplayNameTag = "AA",
        allowUnsafe = true, // KT-85212
    ) {
        classpath += analysisApiRuntimeClasspath
        exclude("**/ObjCExportIntegrationTest.class")
    }
}

projectTests {
    objCExportHeaderGeneratorTestTask(
        "testIntegration",
        allowUnsafe = true, // KT-85212
    ) {
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
