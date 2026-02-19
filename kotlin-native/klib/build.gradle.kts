plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":core:descriptors"))
    implementation(project(":native:frontend.native"))
    implementation(project(":compiler:ir.psi2ir"))
    implementation(project(":compiler:ir.serialization.native"))
    implementation(project(":kotlin-util-klib-abi"))
    implementation(project(":tools:kotlinp-klib"))
    implementation(project(":kotlinx-metadata-klib")) { isTransitive = false }
    implementation(project(":kotlin-metadata")) { isTransitive = false }
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}