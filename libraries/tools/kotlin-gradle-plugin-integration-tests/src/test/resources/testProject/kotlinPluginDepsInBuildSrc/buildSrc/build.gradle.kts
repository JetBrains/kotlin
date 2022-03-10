plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
    mavenLocal()
}

val kotlin_version: String by extra
allprojects {
    dependencies {
        implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:$kotlin_version")
    }
}