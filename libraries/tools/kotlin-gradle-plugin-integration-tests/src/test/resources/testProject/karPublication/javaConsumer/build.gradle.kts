plugins {
    java
}

repositories {
    maven("<localRepo>")
}

dependencies {
    implementation("org.jetbrains.kotlin.kar.test:sample-jvm:1.0")
}
