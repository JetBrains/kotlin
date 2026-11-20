plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    implementation(project(":core:descriptors"))
    implementation(project(":native:frontend.native"))
    implementation(project(":compiler:ir.serialization.native"))
    implementation(project(":kotlin-util-klib-abi"))
    implementation(project(":kotlin-util-klib-metadata"))
    implementation(project(":tools:kotlinp-klib"))
    implementation(project(":native:cinterop.deserialization"))
    implementation(project(":kotlinx-metadata-klib")) { isTransitive = false }
    implementation(project(":kotlin-metadata")) { isTransitive = false }
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}
