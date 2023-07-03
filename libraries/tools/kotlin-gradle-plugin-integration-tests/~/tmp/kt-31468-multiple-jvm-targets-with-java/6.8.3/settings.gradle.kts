
pluginManagement {
    apply(from = "gradle/cache-redirector.settings.gradle.kts")

    repositories {
        mavenLocal()
        mavenCentral()
        google()
        gradlePluginPortal()
    }

    val kotlin_version: String by settings
    val android_tools_version: String by settings
    val test_fixes_version: String by settings
    plugins {
        id("org.jetbrains.kotlin.jvm") version kotlin_version
        id("org.jetbrains.kotlin.kapt") version kotlin_version
        id("org.jetbrains.kotlin.android") version kotlin_version
        id("org.jetbrains.kotlin.js") version kotlin_version
        id("org.jetbrains.kotlin.native.cocoapods") version kotlin_version
        id("org.jetbrains.kotlin.multiplatform") version kotlin_version
        id("org.jetbrains.kotlin.multiplatform.pm20") version kotlin_version
        id("org.jetbrains.kotlin.plugin.allopen") version kotlin_version
        id("org.jetbrains.kotlin.plugin.spring") version kotlin_version
        id("org.jetbrains.kotlin.plugin.jpa") version kotlin_version
        id("org.jetbrains.kotlin.plugin.noarg") version kotlin_version
        id("org.jetbrains.kotlin.plugin.lombok") version kotlin_version
        id("org.jetbrains.kotlin.plugin.sam.with.receiver") version kotlin_version
        id("org.jetbrains.kotlin.plugin.serialization") version kotlin_version
        id("org.jetbrains.kotlin.plugin.assignment") version kotlin_version
        id("org.jetbrains.kotlin.test.fixes.android") version test_fixes_version
        id("org.jetbrains.kotlin.gradle-subplugin-example") version kotlin_version
        id("org.jetbrains.kotlin.plugin.atomicfu") version kotlin_version
        id("org.jetbrains.kotlin.test.gradle-warnings-detector") version test_fixes_version
        id("org.jetbrains.kotlin.test.kotlin-compiler-args-properties") version test_fixes_version
    }
    
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "com.android.application",
                "com.android.library",
                "com.android.test",
                "com.android.dynamic-feature",
                "com.android.asset-pack",
                "com.android.asset-pack-bundle",
                "com.android.lint",
                "com.android.instantapp",
                "com.android.feature",
                "com.android.kotlin.multiplatform.library"
                   -> useModule("com.android.tools.build:gradle:$android_tools_version")
            }
        }
    }
}

plugins {
    id("org.jetbrains.kotlin.test.gradle-warnings-detector")
}

include("lib", "dependsOnPlainJvm", "dependsOnJvmWithJava")