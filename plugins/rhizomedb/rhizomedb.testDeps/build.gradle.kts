import org.jetbrains.kotlin.gradle.dsl.JvmTarget

description = "Fleet RhizomeDB Compiler Plugin (K2)"

plugins {
    kotlin("jvm")
}

repositories {
    maven { setUrl("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies") }
}

dependencies {
    implementation(kotlin("stdlib"))
    compileOnly("org.jetbrains.fleet:util-logging-api:1.36.0-FL24724_a9c22bf7da121.11")
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks.compileJava {
    targetCompatibility = "17"
}

tasks.compileKotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

runtimeJar()
sourcesJar()
javadocJar()
