//buildscript {
//    dependencies {
//        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${property("kotlin_version")}")
//        classpath("org.jetbrains.kotlin:kotlin-sam-with-receiver:${property("kotlin_version")}")
//    }
//}

plugins {
    id("org.jetbrains.kotlin.jvm")
    `kotlin-dsl`
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${property("kotlin_version")}")
}
