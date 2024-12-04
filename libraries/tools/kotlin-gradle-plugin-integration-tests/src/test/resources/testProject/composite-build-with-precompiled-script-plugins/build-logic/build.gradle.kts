plugins {
    `kotlin-dsl`
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

val kotlin_version by properties
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
}