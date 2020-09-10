plugins {
    kotlin("jvm")
}

dependencies {
//    implementation(project(":kotlin-gradle-plugin"))
    implementation("io.ktor:ktor-client-cio:1.4.0")
    implementation("com.google.code.gson:gson:${rootProject.extra["versions.jar.gson"]}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.0.0-RC")
}