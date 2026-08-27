import org.jetbrains.kotlin.testFederation.SmokeTestConfig
import org.jetbrains.kotlin.testFederation.smokeTestConfig

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("java-test-fixtures")
    id("test-inputs-check")
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.backend.konan.InternalKotlinNativeApi")
    }
}

dependencies {
    api(project(":analysis:analysis-api"))
    api(project(":compiler:psi:psi-api"))
    api(project(":native:objcexport-header-generator"))

    implementation(project(":core:compiler.common.native"))
    implementation(project(":kotlin-util-klib"))
    implementation(project(":kotlin-util-klib-metadata"))
    implementation(project(":libraries:tools:analysis-api-based-klib-reader"))
    implementation(project(":native:analysis-api-based-export-common"))

    testRuntimeOnly(libs.junit.platform.launcher)
    testFixturesApi(testFixtures(project(":compiler:tests-common")))
    testFixturesApi(testFixtures(project(":native:objcexport-header-generator")))
    testFixturesApi(project(":native:analysis-api-based-test-utils"))
    testImplementation(project(":analysis:analysis-api-standalone"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

projectTests {
    objCExportHeaderGeneratorTestTask(
        "test",
        allowUnsafe = true, // KT-85212
    ) {
        // Ideally, it should be marked as affected by AnalysisApi instead.
        // But this would be cumbersome and the tests are very fast, so let's keep things simple:
        smokeTestConfig = SmokeTestConfig.RunAllTests
    }
}
