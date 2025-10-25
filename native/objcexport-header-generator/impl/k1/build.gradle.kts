plugins {
    kotlin("jvm")
    id("project-tests-convention")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

dependencies {
    api(project(":native:objcexport-header-generator"))
    testImplementation(project(":compiler:cli-base"))
    implementation(project(":compiler:ir.backend.common"))
    implementation(project(":compiler:ir.objcinterop"))
    implementation(project(":compiler:ir.serialization.native"))
    api(project(":core:descriptors"))
    implementation(project(":native:frontend.native"))
    testImplementation(projectTests(":native:objcexport-header-generator"))
    testImplementation(testFixtures(project(":compiler:tests-common")))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    api(project(":compiler:frontend"))
    api(project(":compiler:frontend.common"))
    api(project(":compiler:psi:psi-api"))
    api(project(":core:compiler.common"))
    api(project(":kotlin-stdlib"))
    api(project(":kotlin-util-io"))
    api(project(":native:base"))
    api(project(":native:binary-options"))
    implementation(intellijCore())
    implementation(project(":compiler:frontend.common.jvm"))
    implementation(project(":compiler:ir.serialization.common"))
    implementation(project(":compiler:psi:psi-frontend-utils"))
    implementation(project(":compiler:psi:psi-impl"))
    implementation(project(":compiler:resolution"))
    implementation(project(":core:util.runtime"))
    implementation(project(":kotlin-tooling-core"))
    implementation(project(":kotlin-util-klib"))
    implementation(project(":kotlin-util-klib-metadata"))
    testImplementation(project(":compiler:cli"))
    testImplementation(project(":compiler:cli-common"))
    testImplementation(project(":compiler:compiler.version"))
    testImplementation(project(":compiler:config"))
    testImplementation(project(":compiler:config.jvm"))
    testImplementation(project(":compiler:frontend.java"))
    testImplementation(project(":compiler:ir.psi2ir"))
    testImplementation(project(":compiler:util"))
    testImplementation(project(":core:deserialization"))
    testImplementation(project(":kotlin-test"))
    testImplementation(testFixtures(project(":compiler:test-infrastructure-utils")))
    testImplementation(testFixtures(project(":compiler:tests-compiler-utils")))
}


optInToK1Deprecation()
optInTo("org.jetbrains.kotlin.backend.konan.InternalKotlinNativeApi")

testsJar()

projectTests {
    objCExportHeaderGeneratorTestTask("test")
}
