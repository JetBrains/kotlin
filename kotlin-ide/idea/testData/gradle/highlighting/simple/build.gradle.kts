plugins {
    id("org.jetbrains.kotlin.jvm") version "1.4.10"
}

repositories {
    mavenCentral()
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }
}
