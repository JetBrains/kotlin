plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
    mavenLocal()
}

val kotlin_version = extra["kotlin_version"]
allprojects {
    dependencies {
        implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:$kotlin_version")
        compileOnly("org.jetbrains.kotlin:kotlin-stdlib:1.7.10")
    }
}
