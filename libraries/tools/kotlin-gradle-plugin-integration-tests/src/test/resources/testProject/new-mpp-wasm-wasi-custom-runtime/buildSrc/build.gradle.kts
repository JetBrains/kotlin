plugins {
    `kotlin-dsl`
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${property("kotlin_version")}")
    implementation("org.tukaani:xz:1.10")
}