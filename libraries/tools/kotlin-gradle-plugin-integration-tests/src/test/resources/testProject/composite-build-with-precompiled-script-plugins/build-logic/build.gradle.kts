plugins {
    `kotlin-dsl`
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

val kotlin_version = extra["kotlin_version"]
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
}
