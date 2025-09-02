plugins {
    `kotlin-dsl`
    id("org.jetbrains.kotlin.jvm")
}

repositories {
    maven(url = "file:///dump")
    mavenCentral { setUrl("https://cache-redirector.jetbrains.com/maven-central") }
}

kotlin.jvmToolchain(8)
