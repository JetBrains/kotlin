plugins {
    kotlin("jvm")
    id("project-tests-convention")
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
    implementation(project(":libraries:tools:analysis-api-based-klib-reader"))
    implementation(project(":native:analysis-api-based-export-common"))

    testImplementation(testFixtures(project(":compiler:tests-common")))
    testImplementation(projectTests(":native:objcexport-header-generator"))
    testImplementation(project(":native:analysis-api-based-test-utils"))
    testImplementation(project(":analysis:analysis-api-standalone"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar()

projectTests {
    objCExportHeaderGeneratorTestTask("test")
}
