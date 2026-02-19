plugins {
    kotlin("jvm")
}

description = "Tool to clean stale compiler caches inside Native distribution"

dependencies {
    implementation(kotlinStdlib())
    implementation(project(":kotlin-native:backend.native")) // for compiler caches code reuse
    implementation(project(":native:kotlin-native-utils"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}