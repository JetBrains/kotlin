import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":kotlin-native:Interop:Indexer"))
    implementation(project(":kotlin-native:Interop:StubGenerator"))
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.optIn.addAll(
            listOf(
                    "kotlinx.cinterop.BetaInteropApi",
                    "kotlinx.cinterop.ExperimentalForeignApi",
            )
    )
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}