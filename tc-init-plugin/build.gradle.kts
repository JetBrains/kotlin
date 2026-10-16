plugins {
    kotlin("jvm") version "2.2.0"
    `java-gradle-plugin`
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
        freeCompilerArgs.addAll("-Xjvm-default=all", "-Xskip-metadata-version-check")
    }
}

repositories {
    maven("https://redirector.kotlinlang.org/maven/kotlin-dependencies")
    mavenCentral()
}

dependencies {
    compileOnly(gradleApi())
}

gradlePlugin {
    plugins {
        register("teamCityInit") {
            id = "org.jetbrains.teamcity.init"
            implementationClass = "org.jetbrains.teamcity.TeamCityInitPlugin"
        }
    }
}
