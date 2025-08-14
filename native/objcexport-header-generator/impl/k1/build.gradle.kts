plugins {
    kotlin("jvm")
    id("compiler-tests-convention")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

dependencies {
    api(project(":native:objcexport-header-generator"))
    implementation(project(":compiler:cli-base"))
    implementation(project(":compiler:ir.backend.common"))
    implementation(project(":compiler:ir.objcinterop"))
    implementation(project(":compiler:ir.serialization.native"))
    implementation(project(":core:descriptors"))
    implementation(project(":native:frontend.native"))
    testImplementation(projectTests(":native:objcexport-header-generator"))
}


optInToK1Deprecation()
optInTo("org.jetbrains.kotlin.backend.konan.InternalKotlinNativeApi")

testsJar()

compilerTests {
    objCExportHeaderGeneratorTestTask("test")
}
