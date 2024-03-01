import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

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
    implementation(project(":kotlin-native:common", configuration = "filesInteropStubs"))
    implementation(project(":kotlin-native:common", configuration = "envInteropStubs"))
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.optIn.addAll(
            listOf(
                    "kotlinx.cinterop.ExperimentalForeignApi",
            )
    )
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}