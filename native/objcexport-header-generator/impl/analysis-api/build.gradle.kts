plugins {
    kotlin("jvm")
}

kotlin {
    compilerOptions {
        /* Required to use Analysis Api */
        freeCompilerArgs.add("-Xcontext-parameters")
        optIn.add("org.jetbrains.kotlin.backend.konan.InternalKotlinNativeApi")
    }
}

dependencies {
    api(project(":analysis:analysis-api"))
    api(project(":compiler:psi"))
    api(project(":native:objcexport-header-generator"))

    implementation(project(":core:compiler.common.native"))
    implementation(project(":kotlin-util-klib"))
    implementation(project(":native:analysis-api-klib-reader"))
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
