plugins {
    kotlin("jvm")
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
}

dependencies {
    api(project(":compiler:ir.serialization.common"))
    api(project(":compiler:ir.tree"))
    api(project(":core:names"))
    api(project(":kotlin-stdlib"))
    implementation(project(":compiler:util"))
    implementation(project(":core:compiler.common"))
    implementation(project(":core:language.model"))
    implementation(project(":core:util.runtime"))
    implementation(project(":core:compiler.common.native"))
    implementation(project(":compiler:ir.serialization.native"))
    implementation(project(":core:descriptors"))
    implementation(project(":kotlinx-metadata-klib"))
    compileOnly(project(":kotlin-metadata")) // Only to fix IDE reporting unresolved references (KTI-3323).
}

optInToUnsafeDuringIrConstructionAPI()
