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
    }

    objCExportHeaderGeneratorTestTask("testAnalysisApi", testDisplayNameTag = "AA") {
        classpath += analysisApiRuntimeClasspath
    }
}

tasks.check.configure {
    dependsOn("testK1")
    dependsOn("testAnalysisApi")
    dependsOn(":native:objcexport-header-generator-k1:check")
    dependsOn(":native:objcexport-header-generator-analysis-api:check")
}
