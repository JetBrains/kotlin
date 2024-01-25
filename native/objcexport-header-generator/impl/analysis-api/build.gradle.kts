plugins {
    kotlin("jvm")
}

kotlin {
    compilerOptions {
        /* Required to use Analysis Api */
        freeCompilerArgs.add("-Xcontext-receivers")
        optIn.add("org.jetbrains.kotlin.backend.konan.InternalKotlinNativeApi")
    }
}

dependencies {
    api(project(":analysis:analysis-api"))
    api(project(":compiler:psi"))
    api(project(":native:objcexport-header-generator"))
    implementation(project(":core:compiler.common.native"))

    testImplementation(projectTests(":native:objcexport-header-generator"))
    testApi(project(":analysis:analysis-api-standalone"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar()

objCExportHeaderGeneratorTest("test")
