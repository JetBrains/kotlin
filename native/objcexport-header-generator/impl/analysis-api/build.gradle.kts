import org.jetbrains.kotlin.testFederation.SmokeTestConfig
import org.jetbrains.kotlin.testFederation.smokeTestConfig

plugins {
    kotlin("jvm")
    id("project-tests-convention")
    id("java-test-fixtures")
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
        smokeTestConfig = SmokeTestConfig.RunAllTests
    }
}
