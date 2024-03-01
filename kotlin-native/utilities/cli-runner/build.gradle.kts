plugins {
    kotlin("jvm")
}

dependencies {
    implementation(kotlinStdlib())
    implementation(project(":kotlin-native:backend.native"))
    implementation(project(":compiler:cli-common"))
    implementation(project(":kotlin-native:Interop:StubGenerator"))
    implementation(project(":kotlin-native:klib"))
    implementation(project(":kotlin-native:endorsedLibraries:kotlinx.cli", configuration = "jvmRuntimeElements"))
    implementation(project(":native:kotlin-native-utils"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}