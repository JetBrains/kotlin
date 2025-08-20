plugins {
    kotlin("jvm")
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

    testImplementation(projectTests(":native:objcexport-header-generator"))
    testApi(project(":native:analysis-api-based-test-utils"))
    testApi(project(":analysis:analysis-api-standalone"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar()

objCExportHeaderGeneratorTest("test")
