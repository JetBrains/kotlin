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
        compileOnly("org.jetbrains.kotlin:kotlin-stdlib:1.7.10")
    }
}

tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileKotlin").configure {
    compilerOptions {
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9)
    }
}