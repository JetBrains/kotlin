import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "Contains the Kotlin compiler plugin for Compose used in Android Studio and IDEA"

dependencies {
    implementation(project(":kotlin-stdlib"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:backend.jvm"))
    compileOnly(project(":compiler:cli-base"))
    compileOnly(project(":compiler:ir.serialization.js"))
    compileOnly(project(":compiler:backend.jvm.codegen"))
    compileOnly(project(":compiler:fir:entrypoint"))

    compileOnly(intellijCore())

    testCompileOnly(project(":compiler:ir.tree"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit4)
    testRuntimeOnly(libs.junit.vintage.engine)
}

optInToUnsafeDuringIrConstructionAPI()
optInToObsoleteDescriptorBasedAPI()

kotlin {
    jvmToolchain(11)
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
