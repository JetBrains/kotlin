plugins {
    kotlin("jvm") apply false
    kotlin("plugin.serialization") apply false
    java
}

repositories {
    mavenLocal()
    mavenCentral()
}

subprojects {
    apply {
        plugin("java")
        plugin("kotlin")
        plugin("org.jetbrains.kotlin.plugin.serialization")
    }

    repositories {
        mavenLocal()
        mavenCentral()
    }

    dependencies {
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    }
}
