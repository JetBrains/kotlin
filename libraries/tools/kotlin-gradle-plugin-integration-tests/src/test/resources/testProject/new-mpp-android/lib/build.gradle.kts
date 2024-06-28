plugins {
    id("com.android.library")
    kotlin("multiplatform")
    `maven-publish`
}

group = "com.example"
version = "1.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

android {
    compileSdk = 31
    namespace = "app.example.com.lib"

    defaultConfig {
        minSdk = 26
        targetSdk = 31

        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation("com.android.support:appcompat-v7:27.1.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("com.android.support.test:runner:1.0.2")
    androidTestImplementation("com.android.support.test.espresso:espresso-core:3.0.2")
}

kotlin.targets.all {
    compilations.all {
        // KT-29964: check that Android compilations can be configured with an `all { ... }` handler:
        kotlinOptions { 
            verbose = true
        }
        compileKotlinTask.doFirst {
            if (!compileKotlinTask.kotlinOptions.verbose) {
                throw AssertionError("kotlinOptions were not configured properly")
            }
            println("KT-29964 OK")
        }
    }
}

kotlin {
    androidTarget("androidLib") {
        attributes {
            attribute(Attribute.of("com.example.target", String::class.java), "androidLib")
        }

        compilations.all {
            attributes {
                attribute(Attribute.of("com.example.compilation", String::class.java), compilationName)
            }
        }

        publishAllLibraryVariants()
    }

    jvm("jvmLib")
    js("jsLib")
}

publishing {
    repositories {
        maven("$buildDir/repo")
    }
}