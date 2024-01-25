plugins {
    kotlin("jvm")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

dependencies {
    api(project(":native:objcexport-header-generator"))
    implementation(project(":compiler:cli-base"))
    implementation(project(":compiler:ir.objcinterop"))
    implementation(project(":compiler:ir.serialization.native"))
    implementation(project(":core:descriptors"))

    testImplementation(projectTests(":native:objcexport-header-generator"))
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.backend.konan.InternalKotlinNativeApi")
    }
}

testsJar()

objCExportHeaderGeneratorTest("test")
