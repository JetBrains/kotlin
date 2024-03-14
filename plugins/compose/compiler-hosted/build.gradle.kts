import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "Contains the Kotlin compiler plugin for Compose used in Android Studio and IDEA"

dependencies {
    implementation(project(":kotlin-stdlib"))
    implementation(project(":js:js.frontend"))
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:backend.jvm"))
    implementation(project(":compiler:cli-base"))
    implementation(project(":compiler:ir.serialization.js"))
    implementation(project(":compiler:backend.jvm.codegen"))
    implementation(project(":compiler:fir:checkers"))
    implementation(project(":compiler:fir:entrypoint"))
    implementation(project(":compiler:fir:tree"))

    compileOnly(intellijCore())

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit4)
    testRuntimeOnly(libs.junit.vintage.engine)
}

optInToUnsafeDuringIrConstructionAPI()
optInToObsoleteDescriptorBasedAPI()

kotlin {
    jvmToolchain(11)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
    }
}

java {
    targetCompatibility = JavaVersion.VERSION_1_8
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<Test> {
    useJUnitPlatform()
}

publish {
    artifactId = "kotlin-compose-compiler-plugin"
    pom {
        name.set("AndroidX Compose Hosted Compiler Plugin")
        developers {
            developer {
                name.set("The Android Open Source Project")
            }
        }
    }
}

standardPublicJars()
